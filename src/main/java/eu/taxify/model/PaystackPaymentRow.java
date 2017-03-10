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
public class PaystackPaymentRow {

    @Id
    @NonNull
    private String reference;
    @NonNull
    private String email;
    @NonNull
    private Double amount;
    @NonNull
    private String status;
    @NonNull
    private LocalDateTime transactionDate;

    @Override
    public String toString() {
        return "{" +
                "reference: " + reference + "," +
                "email: " + email + "," +
                "amount: " + amount + "," +
                "status: " + status + "," +
                "transactionDate: " + transactionDate +
                "}";
    }
}
