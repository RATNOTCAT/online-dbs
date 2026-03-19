package com.sterling.bankportal.repo;

import com.sterling.bankportal.model.SavingsGoal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, String> {
    List<SavingsGoal> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<SavingsGoal> findByIdAndUserId(String id, String userId);
    long countByStatus(String status);
}
