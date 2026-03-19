package com.sterling.bankportal.repo;

import com.sterling.bankportal.model.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, String> {

    List<Beneficiary> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Beneficiary> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndTypeAndAccountNumber(String userId, String type, String accountNumber);

    boolean existsByUserIdAndTypeAndUpiId(String userId, String type, String upiId);
}
