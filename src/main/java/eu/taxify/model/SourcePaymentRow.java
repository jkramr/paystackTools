package eu.taxify.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class SourcePaymentRow {

    @Id
    @NonNull
    private String id;
    @NonNull
    private String type;
    @NonNull
    private LocalDateTime created;
    @NonNull
    private Double amount;
    @NonNull
    private String paymentId;
    @NonNull
    private String userId;
    @NonNull
    private String paymentMethodId;
    @NonNull
    private String paymentMethodType;
    @NonNull
    private String state;
    @NonNull
    private String email;
    @NonNull
    private Integer isAutoRetry;
    @NonNull
    private String previousRowId;
    @NonNull
    private String paystackUserId;

    @Override
    public String toString() {
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
