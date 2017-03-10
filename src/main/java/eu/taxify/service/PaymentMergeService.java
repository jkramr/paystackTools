package eu.taxify.service;

import eu.taxify.model.SourcePaymentRow;
import eu.taxify.model.PaystackPaymentRow;
import eu.taxify.model.User;
import eu.taxify.repository.PaymentRepository;
import eu.taxify.repository.PaystackPaymentRepository;
import eu.taxify.repository.UserRepository;
import lombok.Data;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class PaymentMergeService {

    @Value("${fraudLevel:5000}")
    private Integer fraudLevel;


    private PaymentRepository paymentRepository;
    private PaystackPaymentRepository paystackPaymentRepository;
    private UserRepository userRepository;

    @Autowired
    public PaymentMergeService(
            PaymentRepository paymentRepository,
            PaystackPaymentRepository paystackPaymentRepository,
            UserRepository userRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paystackPaymentRepository = paystackPaymentRepository;
        this.userRepository = userRepository;
    }

    public void initMerge() {

        HashMap<String, String> users = new HashMap<>();
//        HashMap<String, String>  = new HashMap<>();

        paymentRepository.findAll()
                .forEach((row) -> {
                    String userId = row.getUserId();

                    Optional.ofNullable(users.get(userId))
                            .orElseGet(() -> {
                                System.out.println("Processing user: " + userId);

                                return users.put(userId, row.getEmail());
                            });
                });

        users.forEach(this::countExcessivePayments);

        System.out.println("TOTAL users: " + users.size());
    }

    private void countExcessivePayments(String id, String email) {
        StringBuilder paystackPayments = new StringBuilder().append("[ ");
        StringBuilder srcPayments = new StringBuilder().append("[ ");

        MetaData metaData = new MetaData(LocalDateTime.parse("1000-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        Double actuallyCharged = paystackPaymentRepository
                .findByEmail(email)
                .stream()
                .filter(paystackPaymentRow -> paystackPaymentRow.getAmount() != 0.25)
                .map(paystackPaymentRow -> {
                    paystackPayments.append(paystackPaymentRow.toString()).append(", ");
                    return paystackPaymentRow;
                })
                .filter(paystackPaymentRow -> "success".equals(paystackPaymentRow.getStatus()))
                .mapToDouble(PaystackPaymentRow::getAmount)
                .sum();

        double totalRidesVolume = paymentRepository
                .findByUserId(id)
                .stream()
                .filter(sourcePaymentRow -> "NULL".equals(sourcePaymentRow.getPreviousRowId()))
                .map(preparedRow -> {
                            metaData.setPaystackUserId(preparedRow.getPaystackUserId());
                            return paymentRepository.findByPreviousRowId(preparedRow.getId())
                                    .stream()
                                    .filter(sourcePaymentRow -> "capture".equals(sourcePaymentRow.getType()))
                                    .map(sourcePaymentRow -> {
                                        if (sourcePaymentRow.getCreated().isAfter(metaData.getLastRideTime())) {
                                            metaData.setLastRideTime(sourcePaymentRow.getCreated());
                                        }
                                        srcPayments.append(sourcePaymentRow.toString()).append(", ");
                                        return sourcePaymentRow;
                                    })
                                    .findFirst()
                                    .orElse(null);
                        }
                )
                .filter(Objects::nonNull)
                .mapToDouble(SourcePaymentRow::getAmount)
                .sum();

        double totalSuccessfulVolume = paymentRepository
                .findByUserId(id)
                .stream()
                .filter(sourcePaymentRow -> "NULL".equals(sourcePaymentRow.getPreviousRowId()))
                .map(preparedRow -> paymentRepository.findByPreviousRowId(preparedRow.getId())
                        .stream()
                        .filter(sourcePaymentRow -> "capture".equals(sourcePaymentRow.getType()))
                        .filter(sourcePaymentRow -> "finished".equals(sourcePaymentRow.getState()))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .mapToDouble(SourcePaymentRow::getAmount)
                .sum();

        double balance = actuallyCharged - totalRidesVolume;

        System.out.println("id: " + id);
        System.out.println("email: " + email);
        System.out.println("totalRidesVolume: " + totalRidesVolume);
        System.out.println("totalSuccessfulVolume: " + totalSuccessfulVolume);
        System.out.println("actuallyCharged: " + actuallyCharged);
        System.out.println("balance: " + balance);
        System.out.println("lastRideTime: " + metaData.getLastRideTime());

        String resolution = balance == 0
                ? "ok"
                : balance > 0
                    ? "overcharge"
                    : balance > -fraudLevel
                        ? "undercharge"
                        : "fraud";

        User user = new User(
                id,
                email,
                metaData.getPaystackUserId(),
                totalRidesVolume,
                totalSuccessfulVolume,
                actuallyCharged,
                balance,
                resolution,
                metaData.getLastRideTime(),
                trimAndFinalize(srcPayments),
                trimAndFinalize(paystackPayments)
        );

        userRepository.save(user);
    }

    private String trimAndFinalize(StringBuilder builder) {
        return builder
                .delete(builder.length() - 2, builder.length() - 1)
                .append(" ]").toString();
    }

    @Data
    private class MetaData {
        @NonNull
        private LocalDateTime lastRideTime;
        public String paystackUserId;
    }
}
