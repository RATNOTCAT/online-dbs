package com.sterling.bankportal.service;

import com.sterling.bankportal.model.AuditLog;
import com.sterling.bankportal.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog log(String userId, String action, String status, String details) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setStatus(status);
        log.setDetails(details);
        return auditLogRepository.save(log);
    }
}
