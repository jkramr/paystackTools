package eu.taxify.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class User {

  @Id
  @NonNull
  private String id;
  @NonNull
  private String userId;
  @NonNull
  private String email;
  @NonNull
  private String paystackId;
  @NonNull
  private double totalVolume;
  @NonNull
  private double totalSuccessfulVolume;
  @NonNull
  private Double actuallyCharged;
  @NonNull
  private Double balance;
  @NonNull
  private String resolution;
  @NonNull
  private String lastPaymentTime;

  @Lob
  @Column(name = "payments", length = 999999)
  @NonNull
  private String payments;

  @Lob
  @Column(name = "paystackPayments", length = 999999)
  @NonNull
  private String paystackPayments;

  @Override
  public String toString() {
    return
           "\"" + id +
           "\",\"" + userId +
           "\",\"" + email +
           "\",\"" + paystackId +
           "\",\"" + totalVolume +
           "\",\"" + totalSuccessfulVolume +
           "\",\"" + actuallyCharged +
           "\",\"" + balance +
           "\",\"" + resolution +
           "\",\"" + lastPaymentTime +
           "\",\"" + payments +
           "\",\"" + paystackPayments;
  }
}
