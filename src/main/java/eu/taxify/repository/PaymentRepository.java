package eu.taxify.repository;

import eu.taxify.model.SourcePaymentRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Collection;

@RepositoryRestResource
public interface PaymentRepository extends JpaRepository<SourcePaymentRow, String> {

    Collection<SourcePaymentRow> findByUserId(String userId);

    Collection<SourcePaymentRow> findByPreviousRowId(String previousRowId);
}
