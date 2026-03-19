package com.sterling.bankportal.repo;

import com.sterling.bankportal.model.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findFirstByUserId(String userId);
    List<Account> findByUserIdOrderByCreatedAtAsc(String userId);
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
}
