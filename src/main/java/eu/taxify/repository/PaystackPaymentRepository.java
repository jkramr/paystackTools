package eu.taxify.repository;

import eu.taxify.model.PaystackPaymentRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Collection;

@RepositoryRestResource
public interface PaystackPaymentRepository extends JpaRepository<PaystackPaymentRow, String> {

    Collection<PaystackPaymentRow> findByEmail(String generatedEmail);

}
