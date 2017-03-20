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
          = paystackPayment -> "success".equals(paystackPayment.getStatus());

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

  public static PaystackPayment parsePayment(
          String[] fields,
          int[] indices,
          String status,
          String datePattern
  ) {
    return new PaystackPayment(
            // reference
            fields[indices[0]],
            // email
            fields[indices[1]],
            // amount
            Double.valueOf(fields[indices[2]]),
            // status
            status != null ? status : fields[indices[3]],
            // transaction date
            LocalDateTime.parse(
                    fields[indices[4]],
                    DateTimeFormatter.ofPattern(datePattern)
            )
    );
  }

  public static String print(PaystackPayment paystackPayment) {
    return paystackPayment.getAmount() + ": " + paystackPayment.getStatus();
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
}
