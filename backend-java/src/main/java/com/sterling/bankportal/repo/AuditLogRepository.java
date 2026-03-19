package com.sterling.bankportal.repo;

import com.sterling.bankportal.model.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findTop20ByOrderByCreatedAtDesc();
    List<AuditLog> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
