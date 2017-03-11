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
public
class SourcePayment {

  public static final Predicate<SourcePayment>
                                               UNIQUE_ROOT_PAYMENTS_PER_PURCHASE
                                                                   = payment -> "NULL".equals(payment.getPreviousRowId());
  public static final Predicate<SourcePayment> ACTUAL_PAYMENTS     = payment -> "capture".equals(payment.getType());
  public static final Predicate<SourcePayment> SUCCESSFUL_PAYMENTS = payment -> "finished".equals(payment.getState());

  @Id
  @NonNull
  private String        id;
  @NonNull
  private String        type;
  @NonNull
  private LocalDateTime created;
  @NonNull
  private Double        amount;
  @NonNull
  private String        paymentId;
  @NonNull
  private String        userId;
  @NonNull
  private String        paymentMethodId;
  @NonNull
  private String        paymentMethodType;
  @NonNull
  private String        state;
  @NonNull
  private String        email;
  @NonNull
  private Integer       isAutoRetry;
  @NonNull
  private String        previousRowId;
  @NonNull
  private String        paystackUserId;

  public static
  SourcePayment parsePayment(String csvPayment) {
    String[] fields = csvPayment.split(",");

    SourcePayment payment = new SourcePayment(
            fields[0],
            // id
            fields[1],
            // type
            LocalDateTime.parse(fields[4], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            // created: 2017-02-22 15:02:18
            Double.valueOf(fields[5]),
            // amount
            fields[6],
            // payment_id
            fields[7],
            // user_id
            fields[8],
            // payment_method_id
            fields[9],
            // payment_method_type
            fields[10],
            // state
            fields[11],
            // email
            Integer.valueOf(fields[12]),
            // is_auto_retry
            fields[13],
            // previous_row_id
            fields[14]
            // paystack_user_id
    );

    return payment;
  }

  @Override
  public
  String toString() {
    return "{" +
           "id: " + id +
           ", type: " + type +
           ", created=" + created +
           ", amount=" + amount +
           ", paymentId: " + paymentId +
           ", userId: " + userId +
           ", paymentMethodId: " + paymentMethodId +
           ", state: " + state +
           ", email: " + email +
           ", isAutoRetry=" + isAutoRetry +
           ", previousRowId: " + previousRowId +
           ", paystackUserId: " + paystackUserId +
           '}';
  }
}
