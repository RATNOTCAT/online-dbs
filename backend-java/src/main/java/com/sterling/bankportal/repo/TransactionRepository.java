package com.sterling.bankportal.repo;

import com.sterling.bankportal.model.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId);
    List<Transaction> findByAccountIdInOrderByCreatedAtDesc(List<String> accountIds);
    boolean existsByReferenceNumber(String referenceNumber);
}
