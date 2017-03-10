package eu.taxify.service;

import eu.taxify.model.AdminPaymentRow;
import eu.taxify.model.PaystackPaymentRow;
import eu.taxify.model.User;
import eu.taxify.repository.AdminPaymentRepository;
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


    private AdminPaymentRepository adminPaymentRepository;
    private PaystackPaymentRepository paystackPaymentRepository;
    private UserRepository userRepository;

    @Autowired
    public PaymentMergeService(
            AdminPaymentRepository adminPaymentRepository,
            PaystackPaymentRepository paystackPaymentRepository,
            UserRepository userRepository
    ) {
        this.adminPaymentRepository = adminPaymentRepository;
        this.paystackPaymentRepository = paystackPaymentRepository;
        this.userRepository = userRepository;
    }

    public void initMerge() {

        HashMap<String, String> users = new HashMap<>();
//        HashMap<String, String>  = new HashMap<>();

        adminPaymentRepository.findAll()
                .forEach((row) -> {
                    String userId = row.getUserId();

                    Optional.ofNullable(users.get(userId))
                            .orElseGet(() -> {
                                System.out.println("Processing user: " + userId);

                                return users.put(userId, row.getGeneratedEmail());
                            });
                });

        users.forEach(this::countExcessivePayments);

        System.out.println("TOTAL users: " + users.size());
    }

    private void countExcessivePayments(String id, String email) {
        StringBuilder paystackPayments = new StringBuilder().append("[ ");
        StringBuilder adminPayments = new StringBuilder().append("[ ");

        Ride ride = new Ride(LocalDateTime.parse("1000-01-01 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));


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

        double totalRidesVolume = adminPaymentRepository
                .findByUserId(id)
                .stream()
                .filter(adminPaymentRow -> "NULL".equals(adminPaymentRow.getPreviousRowId()))
                .map(preparedRow -> adminPaymentRepository.findByPreviousRowId(preparedRow.getId())
                        .stream()
                        .filter(adminPaymentRow -> "capture".equals(adminPaymentRow.getType()))
                        .map(adminPaymentRow -> {
                            if (adminPaymentRow.getCreated().isAfter(ride.getLastRideTime())) {
                                ride.setLastRideTime(adminPaymentRow.getCreated());
                            }
                            adminPayments.append(adminPaymentRow.toString()).append(", ");
                            return adminPaymentRow;
                        })
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .mapToDouble(AdminPaymentRow::getAmount)
                .sum();

        double totalSuccessfulVolume = adminPaymentRepository
                .findByUserId(id)
                .stream()
                .filter(adminPaymentRow -> "NULL".equals(adminPaymentRow.getPreviousRowId()))
                .map(preparedRow -> adminPaymentRepository.findByPreviousRowId(preparedRow.getId())
                        .stream()
                        .filter(adminPaymentRow -> "capture".equals(adminPaymentRow.getType()))
                        .filter(adminPaymentRow -> "finished".equals(adminPaymentRow.getState()))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .mapToDouble(AdminPaymentRow::getAmount)
                .sum();

        double balance = actuallyCharged - totalRidesVolume;

        System.out.println("id: " + id);
        System.out.println("email: " + email);
        System.out.println("totalRidesVolume: " + totalRidesVolume);
        System.out.println("totalSuccessfulVolume: " + totalSuccessfulVolume);
        System.out.println("actuallyCharged: " + actuallyCharged);
        System.out.println("balance: " + balance);
        System.out.println("lastRideTime: " + ride.getLastRideTime());

        String resolution = actuallyCharged > totalRidesVolume
                ? "overcharge"
                : actuallyCharged - totalRidesVolume >= -fraudLevel
                ? "undercharge"
                : actuallyCharged - totalRidesVolume >= -fraudLevel
                ? "fraud"
                : actuallyCharged == totalRidesVolume
                ? "ok"
                : "miracle";

        User user = new User(
                id,
                email,
                totalRidesVolume,
                totalSuccessfulVolume,
                actuallyCharged,
                balance,
                resolution,
                ride.getLastRideTime(),
                trim(adminPayments),
                trim(paystackPayments)
        );

        userRepository.save(user);
    }

    private String trim(StringBuilder builder) {
        return builder
                .delete(builder.length() - 2, builder.length() - 1)
                .append(" ]").toString();
    }

    @Data
    private class Ride {
        @NonNull
        private LocalDateTime lastRideTime;
    }
}
