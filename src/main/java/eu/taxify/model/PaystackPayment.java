package eu.taxify.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class PaystackPayment {

  public static final double ADD_CARD_FEE = 0.25;

  public static final Predicate<PaystackPayment>
          PURCHASE_PAYSTACK_PAYMENTS
          = paystackPayment -> paystackPayment.getAmount() !=
                               ADD_CARD_FEE;
  public static final Predicate<PaystackPayment>
          SUCCESSFUL_PAYSTACK_PAYMENTS
          = paystackPayment -> "success".equals(paystackPayment
                                                        .getStatus());

  @Id
  @NonNull
  private String        reference;
  @NonNull
  private String        email;
  @NonNull
  private Double        amount;
  @NonNull
  private String        status;
  @NonNull
  private LocalDateTime transactionDate;

  public static PaystackPayment parseCSVPayment(String csvString) {
    String[] fields = csvString.split(",");

    return new PaystackPayment(
            fields[0],
            // reference
            fields[1],
            // email
            Double.valueOf(fields[2]),
            // amount
            fields[3],
            // status
            LocalDateTime.parse(
                    fields[4],
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm:ss")
            )
            // transaction date: 2017-02-22 15:02:18
    );
  }

  @Override
  public String toString() {
    return "{" +
           "reference: " + reference + ", " +
           "email: " + email + ", " +
           "amount: " + amount + ", " +
           "status: " + status + ", " +
           "transactionDate: " + transactionDate +
           "}";
  }

  public String toShortString() {
    return amount + ": " + status;
  }

  public static String print(PaystackPayment paystackPayment) {
    return paystackPayment.getAmount() + ": " + paystackPayment.getStatus();
  }
}
