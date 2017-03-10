package eu.taxify.repository;

import eu.taxify.model.AdminPaymentRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Collection;

@RepositoryRestResource
public interface AdminPaymentRepository extends JpaRepository<AdminPaymentRow, String> {

    Collection<AdminPaymentRow> findByUserId(String userId);

    Collection<AdminPaymentRow> findByPreviousRowId(String previousRowId);
}
