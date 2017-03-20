package eu.taxify.service;

import eu.taxify.model.PaystackPayment;
import eu.taxify.model.SourcePayment;
import eu.taxify.model.User;
import eu.taxify.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class PaymentMergeService {

  private final PaymentService<SourcePayment>   paymentService;
  private final PaymentService<PaystackPayment> paystackService;

  @Value("${fraudLevel:5000}")
  private Integer fraudLevel;

  @Value("${fullView:true}")
  private Boolean fullView;

  private Logger logger;
  private UserRepository userRepository;

  private HashMap<String, UserPayments>         users;
  private HashMap<String, UserPaystackPayments> paystackUsers;

  @Autowired
  public PaymentMergeService(
          Logger logger,
          UserRepository userRepository,
          PaymentService<SourcePayment> sourcePaymentService,
          PaymentService<PaystackPayment> paystackPaymentService
  ) {
    this.logger = logger;
    this.userRepository = userRepository;
    this.paymentService = sourcePaymentService;
    this.paystackService = paystackPaymentService;
  }

  public void init(Consumer<String> log) {
    users = new HashMap<>();
    paystackUsers = new HashMap<>();

    paymentService.consume(payment -> putPayment(
            users,
            paystackUsers,
            payment
    ));
    paystackService.consume(payment -> putPaystackPayment(
            paystackUsers,
            payment
    ));

    log.accept("Done");
  }

  public void run(Consumer<String> log) {
    init(log);

    users.forEach((id, userPayments) -> {
      String email = userPayments.getEmail();

      UserPaystackPayments paystackPayments = paystackUsers.get(email);

      User user = resolveUserPayments(
              id,
              userPayments,
              paystackPayments
      );

      log.accept(user.toString());
    });

    log.accept("Total users: " + users.size());
  }

  private User resolveUserPayments(
          String id,
          UserPayments userPayments,
          UserPaystackPayments paystackPayments
  ) {
    return Optional.ofNullable(paystackPayments)
                   .map(UserPaystackPayments::getList)
                   .map(userPaystackPayments -> processUserPurchases(
                           id,
                           userPayments,
                           userPaystackPayments
                   ))
                   .orElse(null);
  }

  private User processUserPurchases(
          String id,
          UserPayments userPayments,
          Collection<PaystackPayment> userPaystackPayments
  ) {
    UserPaymentsData userPaymentsData = new UserPaymentsData();

    ArrayList<Double> purchases = new ArrayList<>();

    userPayments.getList()
                .stream()
                .filter(SourcePayment.UNIQUE_ROOT_PAYMENTS_PER_PURCHASE)
                .forEach(purchase -> processPurchaseActions(
                        userPaymentsData,
                        userPayments,
                        purchase,
                        purchases
                ));

    userPaystackPayments
            .stream()
            .filter(PaystackPayment.PURCHASE_PAYSTACK_PAYMENTS)
            .map(paystackPayment -> processCharge(
                    userPaymentsData,
                    paystackPayment
            ))
            .filter(PaystackPayment.SUCCESSFUL_PAYSTACK_PAYMENTS)
            .forEach(paystackPayment -> processSuccessfulCharge(
                    userPaymentsData,
                    paystackPayment
            ));

    Double balance = userPaymentsData.getActuallyCharged() -
                     userPaymentsData.getTotalVolume();

    String resolution = balance == 0
                        ? "ok"
                        : balance > 0
                          ? "overcharge"
                          : balance > -fraudLevel
                            ? "undercharge"
                            : "fraud";

    User user = new User(
            id,
            id,
            userPayments.getEmail(),
            userPayments.getPaystackId(),
            userPaymentsData.getTotalVolume(),
            userPaymentsData.getTotalSuccessfulVolume(),
            userPaymentsData.getActuallyCharged(),
            balance,
            resolution,
            userPaymentsData.getLastPaymentTime()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            userPaymentsData.stringPayments(),
            userPaymentsData.stringPaystackPayments()
    );

    userRepository.save(user);

    return user;
  }

  private void processPurchaseActions(
          UserPaymentsData userPaymentsData,
          UserPayments userPayments,
          SourcePayment purchaseRow,
          ArrayList<Double> purchases
  ) {
    Amounts amounts = new Amounts();

    userPayments.getList()
                .stream()
                // filter children of this purchase
                .filter(payment -> purchaseRow.getId()
                                              .equals(payment.getPreviousRowId()))
                .filter(SourcePayment.ACTUAL_PAYMENTS)
                .map(payment -> processAction(
                        userPaymentsData,
                        amounts,
                        payment,
                        purchases
                ))
                .filter(SourcePayment.SUCCESSFUL_PAYMENTS)
                .forEach(payment -> processSuccessfulAction(
                        amounts,
                        payment,
                        purchases
                ));

    userPaymentsData.addToTotal(amounts.purchaseAmount);
    userPaymentsData.addToSuccessfulTotal(amounts.successfulPurchaseAmount);
  }

  private void processSuccessfulAction(
          Amounts amounts,
          SourcePayment payment,
          ArrayList<Double> purchases
  ) {
    if (amounts.successfulPurchaseAmount == 0.0) {
      amounts.successfulPurchaseAmount = payment.getAmount();
    } else {
      String message = "broken state payment-paystack";
      logger.error(message);
      throw new IllegalStateException(message);
    }
  }

  private SourcePayment processAction(
          UserPaymentsData userPaymentsData,
          Amounts amounts,
          SourcePayment sourcePayment,
          ArrayList<Double> purchases
  ) {
    if (sourcePayment.getCreated()
                     .isAfter(userPaymentsData.getLastPaymentTime())) {
      userPaymentsData.setLastPaymentTime(sourcePayment.getCreated());
    }

    if (amounts.purchaseAmount == 0.0) {
      purchases.add(sourcePayment.getAmount());
      amounts.purchaseAmount = sourcePayment.getAmount();
    }

    return userPaymentsData.addToPayments(sourcePayment);
  }

  private PaystackPayment processCharge(
          UserPaymentsData userPaymentsData,
          PaystackPayment paystackPayment
  ) {
    return userPaymentsData.addToPaystackPayments(paystackPayment);
  }

  private void processSuccessfulCharge(
          UserPaymentsData userPaymentsData,
          PaystackPayment paystackPayment
  ) {
    userPaymentsData.addToActuallyCharged(paystackPayment.getAmount());
  }

  private void putPayment(
          HashMap<String, UserPayments> payments,
          HashMap<String, UserPaystackPayments> paystackPayments,
          SourcePayment sourcePayment
  ) {
    String userId     = sourcePayment.getUserId();
    String email      = sourcePayment.getEmail();
    String paystackId = sourcePayment.getPaystackUserId();

    payments.putIfAbsent(
            userId,
            new UserPayments(
                    userId,
                    email,
                    paystackId,
                    new ArrayList<>()
            )
    );
    paystackPayments.putIfAbsent(
            email,
            new UserPaystackPayments(
                    userId,
                    email,
                    paystackId,
                    new ArrayList<>()
            )
    );

    payments.get(userId).getList().add(sourcePayment);
  }

  private Boolean putPaystackPayment(
          HashMap<String, UserPaystackPayments> paystackPayments,
          PaystackPayment paystackPayment
  ) {
    String email = paystackPayment.getEmail();

    return Optional.ofNullable(paystackPayments.get(email))
                   .map(userPaystackPayments ->
                                userPaystackPayments.getList()
                                                    .add(paystackPayment))
                   .orElse(false);
  }

  @Data
  private class UserPaymentsData {
    StringBuffer srcPayments      = new StringBuffer().append("[ ");
    StringBuffer paystackPayments = new StringBuffer().append("[ ");
    LocalDateTime
                 lastPaymentTime
                                  = LocalDateTime.parse(
            "1000-01-01 00:00:00",
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    );

    Double actuallyCharged       = 0.0;
    Double totalVolume           = 0.0;
    Double totalSuccessfulVolume = 0.0;

    private String trimAndFinalize(StringBuffer builder) {
      return builder.length() <= 2
             ? "[ ]"
             : builder
                     .delete(builder.length() - 2, builder.length() - 1)
                     .append(" ]").toString();
    }

    String stringPayments() {
      return trimAndFinalize(srcPayments);
    }

    String stringPaystackPayments() {
      return trimAndFinalize(paystackPayments);
    }

    void addToTotal(Double purchaseAmount) {
      this.totalVolume += purchaseAmount;
    }

    void addToSuccessfulTotal(Double successfulPurchaseAmount) {
      this.totalSuccessfulVolume += successfulPurchaseAmount;
    }

    PaystackPayment addToPaystackPayments(PaystackPayment paystackPayment) {
      this.paystackPayments.append(fullView
                                   ? paystackPayment.toString()
                                   : paystackPayment.toShortString()
      ).append(", ");

      return paystackPayment;
    }

    SourcePayment addToPayments(SourcePayment sourcePayment) {
      this.srcPayments.append(fullView
                              ? sourcePayment.toString()
                              : sourcePayment.toShortString()
      ).append(", ");

      return sourcePayment;
    }

    void addToActuallyCharged(Double chargeAmount) {
      this.actuallyCharged += chargeAmount;
    }
  }

  @Data
  @AllArgsConstructor
  private class UserPayments {
    String                    userId;
    String                    email;
    String                    paystackId;
    Collection<SourcePayment> list;
  }

  @Data
  @AllArgsConstructor
  private class UserPaystackPayments {
    String                      userId;
    String                      email;
    String                      paystackId;
    Collection<PaystackPayment> list;
  }

  private class Amounts {
    Double purchaseAmount           = 0.0;
    Double successfulPurchaseAmount = 0.0;
  }
}
