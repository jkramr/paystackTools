package eu.taxify.repository;

import eu.taxify.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Collection;

@RepositoryRestResource
public interface UserRepository
        extends JpaRepository<User, String> {

  Collection<User> findByResolution(@Param("resolution") String resolution);

  Collection<User> findByPaystackId(@Param("id") String id);

  Collection<User> findByBalance(@Param("balance") Double balance);

  Collection<User> findByBalanceGreaterThan(@Param("balance") Double balance);

  Collection<User> findByBalanceLessThan(@Param("balance") Double balance);

  Collection<User> findByEmail(@Param("email") String email);
}
