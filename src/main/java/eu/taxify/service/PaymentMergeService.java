package eu.taxify.service;

import eu.taxify.model.SourcePayment;
import eu.taxify.model.PaystackPayment;
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
public class PaymentMergeService {

    @Value("${fraudLevel:5000}")
    private Integer fraudLevel;

    private UserRepository userRepository;

    @Autowired
    public PaymentMergeService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    public void initMerge(SimpleFileReader srcFileReader, SimpleFileReader paystackFileReader) {

        HashMap<String, UserPayments> users = new HashMap<>();
        HashMap<String, UserPaystackPayments> paystackUsers = new HashMap<>();

        readFile(srcFileReader, payment -> putPayment(users, paystackUsers, payment));

        readFile(paystackFileReader, payment -> putPaystackPayment(paystackUsers, payment));

        users.forEach((id, userPayments) -> {
            String email = userPayments.getEmail();

            System.out.println("Processing user: {id: " + id + ", email: " + userPayments.getEmail());

            String resolution = Optional.ofNullable(paystackUsers.get(email))
                    .map(UserPaystackPayments::getList)
                    .map(userPaystackPayments -> processUserPurchases(id, userPayments, userPaystackPayments))
                    .orElse("not matched with internal user");

            System.out.println("User resolved: " + resolution);
        });

        System.out.println("TOTAL users: " + users.size());
    }

    private String processUserPurchases(String id, UserPayments userPayments, Collection<PaystackPayment> userPaystackPayments) {
        UserPaymentsData userPaymentsData = new UserPaymentsData();

        userPaystackPayments
                .stream()
                .filter(PaystackPayment.PURCHASE_PAYSTACK_PAYMENTS)
                .map(userPaymentsData::addToPaystackPayments)
                .filter(PaystackPayment.SUCCESSFUL_PAYSTACK_PAYMENTS)
                .mapToDouble(PaystackPayment::getAmount)
                .forEach(userPaymentsData::addToActuallyCharged);

        userPayments.getList()
                .stream()
                .filter(SourcePayment.UNIQUE_ROOT_PAYMENTS_PER_PURCHASE)
                .forEach(purchase -> processPurchaseActions(userPaymentsData, userPayments, purchase));

        double balance = userPaymentsData.getActuallyCharged() - userPaymentsData.getTotalVolume();

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
                userPaymentsData.getPaystackUserId(),
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

    private void processPurchaseActions(UserPaymentsData userPaymentsData, UserPayments userPayments, SourcePayment purchaseRow) {
        Amounts amounts = new Amounts();
        String purchaseRowId = purchaseRow.getId();

        userPayments.getList()
                .stream()
                .filter(payment -> purchaseRowId.equals(payment.getPreviousRowId()))
                .filter(SourcePayment.ACTUAL_PAYMENTS)
                .map(sourcePayment -> processPayment(userPaymentsData, amounts, sourcePayment))
                .filter(SourcePayment.SUCCESSFUL_PAYMENTS)
                .forEach(sourcePayment -> amounts.successfulPurchaseAmount = sourcePayment.getAmount());

        userPaymentsData.addToTotal(amounts.purchaseAmount);
        userPaymentsData.addToSuccessfulTotal(amounts.successfulPurchaseAmount);
        userPaymentsData.setPaystackUserId(purchaseRow.getPaystackUserId());
    }

    private SourcePayment processPayment(UserPaymentsData userPaymentsData, Amounts amounts, SourcePayment sourcePayment) {
        if (sourcePayment.getCreated().isAfter(userPaymentsData.getLastPaymentTime())) {
            userPaymentsData.setLastPaymentTime(sourcePayment.getCreated());
        }

        amounts.purchaseAmount = sourcePayment.getAmount();

        return userPaymentsData.addToPayments(sourcePayment);
    }

    private void putPayment(HashMap<String, UserPayments> payments, HashMap<String, UserPaystackPayments> paystackPayments, String payment) {
        SourcePayment sourcePayment = SourcePayment.parsePayment(payment);

        String userId = sourcePayment.getUserId();
        String email = sourcePayment.getEmail();

        payments.putIfAbsent(userId, new UserPayments(userId, email, new ArrayList<>()));
        paystackPayments.putIfAbsent(email, new UserPaystackPayments(userId, email, new ArrayList<>()));

        payments.get(userId).getList().add(sourcePayment);

//                    paymentRepository.save(sourcePayment);
    }

    private Boolean putPaystackPayment(HashMap<String, UserPaystackPayments> paystackPayments, String payment) {
        PaystackPayment paystackPayment = PaystackPayment.parseCSVPayment(payment);

        String email = paystackPayment.getEmail();

        return Optional.ofNullable(paystackPayments.get(email))
                .map(userPaystackPayments -> userPaystackPayments.getList().add(paystackPayment))
                .orElse(false);
    }

    private void readFile(SimpleFileReader paystackFileReader, Consumer<String> action) {
        Arrays.stream(paystackFileReader.readFile()
                .split("\n"))
                .forEach(action);
    }

    @Data
    private class UserPaymentsData {
        StringBuffer srcPayments = new StringBuffer().append("[ ");
        StringBuffer paystackPayments = new StringBuffer().append("[ ");
        LocalDateTime lastPaymentTime = LocalDateTime.parse("1000-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String paystackUserId;

        Double actuallyCharged = 0.0;
        Double totalVolume = 0.0;
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
    private class UserPayments {
        String userId;
        String email;
        Collection<SourcePayment> list;
    }

    @Data
    @AllArgsConstructor
    private class UserPaystackPayments {
        String userId;
        String email;
        Collection<PaystackPayment> list;
    }

    private class Amounts {
        Double purchaseAmount = 0.0;
        Double successfulPurchaseAmount = 0.0;

    }
}
