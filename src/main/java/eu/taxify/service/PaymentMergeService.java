package eu.taxify.service;

import eu.taxify.model.PaystackPayment;
import eu.taxify.model.SourcePayment;
import eu.taxify.model.User;
import eu.taxify.repository.UserRepository;
import eu.taxify.util.SimpleFileReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

@Component
public
class PaymentMergeService {

  @Value("${fraudLevel:5000}")
  private Integer fraudLevel;

  private UserRepository userRepository;

  private HashMap<String, UserPayments>         users;
  private HashMap<String, UserPaystackPayments> paystackUsers;

  @Autowired
  public
  PaymentMergeService(
          UserRepository userRepository
  ) {
    this.userRepository = userRepository;
  }

  public
  void init(SimpleFileReader srcFileReader, SimpleFileReader paystackFileReader) {
    users = new HashMap<>();
    paystackUsers = new HashMap<>();

    readFile(srcFileReader, payment -> putPayment(users, paystackUsers, payment));

    readFile(paystackFileReader, payment -> putPaystackPayment(paystackUsers, payment));
  }

  public
  void run(Consumer<String> out) {
    users.forEach((id, userPayments) -> {
      String               email            = userPayments.getEmail();
      UserPaystackPayments paystackPayments = paystackUsers.get(email);

      out.accept("User: {");
      out.accept("id: " + id + ", \n" +
                 "email: " + userPayments.getEmail() + ", \n" +
                 "paystackId: " + userPayments.getPaystackId() + ","
      );

      String resolution = resolveUserPayments(id, userPayments, paystackPayments, out);

      out.accept("resolution: " + resolution);

      out.accept("}");
    });

    out.accept("Total users: " + users.size());
  }

  public
  String resolveUserPayments(
          String id,
          UserPayments userPayments,
          UserPaystackPayments paystackPayments,
          Consumer<String> out
  ) {
    return Optional.ofNullable(paystackPayments)
                   .map(UserPaystackPayments::getList)
                   .map(userPaystackPayments -> processUserPurchases(id, userPayments, userPaystackPayments, out))
                   .orElse("not matched with internal user");
  }

  private
  String processUserPurchases(
          String id,
          UserPayments userPayments,
          Collection<PaystackPayment> userPaystackPayments,
          Consumer<String> out
  ) {
    UserPaymentsData userPaymentsData = new UserPaymentsData();

    out.accept("purchases: [");

    userPayments.getList()
                .stream()
                .filter(SourcePayment.UNIQUE_ROOT_PAYMENTS_PER_PURCHASE)
                .forEach(purchase -> processPurchaseActions(userPaymentsData, userPayments, purchase, out));

    out.accept("],");

    out.accept("paystack_payments: {");

    userPaystackPayments
            .stream()
            .filter(PaystackPayment.PURCHASE_PAYSTACK_PAYMENTS)
            .map(paystackPayment -> processCharge(userPaymentsData, paystackPayment, out))
            .filter(PaystackPayment.SUCCESSFUL_PAYSTACK_PAYMENTS)
            .mapToDouble(PaystackPayment::getAmount)
            .forEach(chargeAmount -> processSuccessfulCharge(userPaymentsData, chargeAmount, out));

    out.accept("},");

    Double balance = userPaymentsData.getActuallyCharged() - userPaymentsData.getTotalVolume();

    String resolution = balance == 0
                        ? "ok"
                        : balance > 0
                          ? "overcharge"
                          : balance > -fraudLevel
                            ? "undercharge"
                            : "fraud";

    userRepository.save(new User(
            id,
            userPayments.getEmail(),
            userPayments.getPaystackId(),
            userPaymentsData.getTotalVolume(),
            userPaymentsData.getTotalSuccessfulVolume(),
            userPaymentsData.getActuallyCharged(),
            balance,
            resolution,
            userPaymentsData.getLastPaymentTime(),
            userPaymentsData.stringPayments(),
            userPaymentsData.stringPaystackPayments()
    ));

    return resolution;
  }

  private
  void processPurchaseActions(
          UserPaymentsData userPaymentsData,
          UserPayments userPayments,
          SourcePayment purchaseRow,
          Consumer<String> out
  ) {
    Amounts amounts = new Amounts();

    userPayments.getList()
                .stream()
                // filter children of this purchase
                .filter(payment -> purchaseRow.getId().equals(payment.getPreviousRowId()))
                .filter(SourcePayment.ACTUAL_PAYMENTS)
                .map(payment -> processAction(userPaymentsData, amounts, payment, out))
                .filter(SourcePayment.SUCCESSFUL_PAYMENTS)
                .forEach(payment -> processSuccessfulAction(amounts, payment, out));

    userPaymentsData.addToTotal(amounts.purchaseAmount);
    userPaymentsData.addToSuccessfulTotal(amounts.successfulPurchaseAmount);
  }

  private
  void processSuccessfulAction(
          Amounts amounts,
          SourcePayment payment,
          Consumer<String> out
  ) {
    if (amounts.successfulPurchaseAmount == 0.0) {
      amounts.successfulPurchaseAmount = payment.getAmount();
    } else {
      out.accept("______________BROKEN_STATE______________");
    }
  }

  private
  SourcePayment processAction(
          UserPaymentsData userPaymentsData,
          Amounts amounts,
          SourcePayment sourcePayment,
          Consumer<String> out
  ) {
    if (sourcePayment.getCreated().isAfter(userPaymentsData.getLastPaymentTime())) {
      userPaymentsData.setLastPaymentTime(sourcePayment.getCreated());
    }

    if (amounts.purchaseAmount == 0.0) {
      out.accept(sourcePayment.getAmount() + ",");
      amounts.purchaseAmount = sourcePayment.getAmount();
    }

    return userPaymentsData.addToPayments(sourcePayment);
  }

  private
  PaystackPayment processCharge(
          UserPaymentsData userPaymentsData,
          PaystackPayment paystackPayment,
          Consumer<String> out
  ) {
    if ("failed".equals(paystackPayment.getStatus())) {
      out.accept(paystackPayment.getAmount() + ": failed,");
    }
    return userPaymentsData.addToPaystackPayments(paystackPayment);
  }

  private
  void processSuccessfulCharge(
          UserPaymentsData userPaymentsData, double chargeAmount, Consumer<String> out
  ) {
    out.accept(chargeAmount + ": charged,");

    userPaymentsData.addToActuallyCharged(chargeAmount);
  }

  private
  void putPayment(
          HashMap<String, UserPayments> payments,
          HashMap<String, UserPaystackPayments> paystackPayments,
          String payment
  ) {
    SourcePayment sourcePayment = SourcePayment.parsePayment(payment);

    String userId     = sourcePayment.getUserId();
    String email      = sourcePayment.getEmail();
    String paystackId = sourcePayment.getPaystackUserId();

    payments.putIfAbsent(userId, new UserPayments(userId, email, paystackId, new ArrayList<>()));
    paystackPayments.putIfAbsent(email, new UserPaystackPayments(userId, email, paystackId, new ArrayList<>()));

    payments.get(userId).getList().add(sourcePayment);

//                    paymentRepository.save(sourcePayment);
  }

  private
  Boolean putPaystackPayment(HashMap<String, UserPaystackPayments> paystackPayments, String payment) {
    PaystackPayment paystackPayment = PaystackPayment.parseCSVPayment(payment);

    String email = paystackPayment.getEmail();

    return Optional.ofNullable(paystackPayments.get(email))
                   .map(userPaystackPayments -> userPaystackPayments.getList().add(paystackPayment))
                   .orElse(false);
  }

  private
  void readFile(SimpleFileReader paystackFileReader, Consumer<String> action) {
    Arrays.stream(paystackFileReader.readFile()
                                    .split("\n"))
          .forEach(action);
  }

  @Data
  private
  class UserPaymentsData {
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

    private
    String trimAndFinalize(StringBuffer builder) {
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
      this.paystackPayments.append(paystackPayment.toString()).append(", ");
      return paystackPayment;
    }

    SourcePayment addToPayments(SourcePayment sourcePayment) {
      this.srcPayments.append(sourcePayment.toString()).append(", ");
      return sourcePayment;
    }

    void addToActuallyCharged(Double chargeAmount) {
      this.actuallyCharged += chargeAmount;
    }
  }

  @Data
  @AllArgsConstructor
  private
  class UserPayments {
    String                    userId;
    String                    email;
    String                    paystackId;
    Collection<SourcePayment> list;
  }

  @Data
  @AllArgsConstructor
  private
  class UserPaystackPayments {
    String                      userId;
    String                      email;
    String                      paystackId;
    Collection<PaystackPayment> list;
  }

  private
  class Amounts {
    Double purchaseAmount           = 0.0;
    Double successfulPurchaseAmount = 0.0;

  }
}
