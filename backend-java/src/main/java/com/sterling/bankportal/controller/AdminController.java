package com.sterling.bankportal.controller;

import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.NotificationRepository;
import com.sterling.bankportal.repo.SavingsGoalRepository;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.AuditLogRepository;
import com.sterling.bankportal.repo.BeneficiaryRepository;
import com.sterling.bankportal.repo.TransactionRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.service.AuditLogService;
import com.sterling.bankportal.service.NotificationService;
import com.sterling.bankportal.util.ApiResponse;
import java.security.Principal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin", "/api/manage"})
public class AdminController extends BaseController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AdminController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            BeneficiaryRepository beneficiaryRepository,
            AuditLogRepository auditLogRepository,
            NotificationRepository notificationRepository,
            SavingsGoalRepository savingsGoalRepository,
            NotificationService notificationService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Object> summary(Principal principal) {
        User admin = requireAdmin(principal);
        if (admin == null) {
            return error(HttpStatus.FORBIDDEN, "Admin access required");
        }
        Map<String, Object> response = ApiResponse.success();
        Map<String, Object> summary = new LinkedHashMap<>();
        var transactions = transactionRepository.findAll();
        var notifications = notificationRepository.findAll();
        var goals = savingsGoalRepository.findAll();
        summary.put("total_users", userRepository.count());
        summary.put("total_accounts", accountRepository.count());
        summary.put("total_transactions", transactions.size());
        summary.put("total_beneficiaries", beneficiaryRepository.count());
        summary.put("total_notifications", notifications.size());
        summary.put("unread_notifications", notifications.stream().filter(notification -> !notification.isRead()).count());
        summary.put("total_goals", goals.size());
        summary.put("completed_goals", goals.stream().filter(goal -> "completed".equalsIgnoreCase(goal.getStatus())).count());
        summary.put("goal_target_total", goals.stream().mapToDouble(goal -> goal.getTargetAmount()).sum());
        summary.put("goal_saved_total", goals.stream().mapToDouble(goal -> goal.getSavedAmount()).sum());
        summary.put("locked_users", userRepository.findAll().stream().filter(u -> u.getLockedUntil() != null && u.getLockedUntil().isAfter(java.time.LocalDateTime.now())).count());
        summary.put("recent_audits", auditLogRepository.findTop20ByOrderByCreatedAtDesc());
        summary.put("recent_notifications", notifications.stream().sorted(Comparator.comparing(com.sterling.bankportal.model.Notification::getCreatedAt).reversed()).limit(10).map(notification -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", notification.getId());
            item.put("title", notification.getTitle());
            item.put("type", notification.getType());
            item.put("is_read", notification.isRead());
            item.put("created_at", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null);
            return item;
        }).toList());
        summary.put("transaction_method_breakdown", transactions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        transaction -> {
                            String key = transaction.getMethod();
                            return key == null || key.isBlank() ? "other" : key;
                        },
                        java.util.stream.Collectors.summingDouble(transaction -> transaction.getAmount())))
                .entrySet()
                .stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("method", entry.getKey());
                    item.put("amount", entry.getValue());
                    return item;
                })
                .toList());
        summary.put("monthly_transaction_volume", transactions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        transaction -> transaction.getCreatedAt() != null
                                ? transaction.getCreatedAt().getYear() + "-" + String.format("%02d", transaction.getCreatedAt().getMonthValue())
                                : "unknown",
                        java.util.stream.Collectors.summingDouble(transaction -> transaction.getAmount())))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("month", entry.getKey());
                    item.put("amount", entry.getValue());
                    return item;
                })
                .toList());
        summary.put("users", userRepository.findAll().stream().map(this::userPayload).toList());
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/unlock")
    @Transactional
    public ResponseEntity<Object> unlockUser(Principal principal, @PathVariable String userId) {
        User admin = requireAdmin(principal);
        if (admin == null) {
            return error(HttpStatus.FORBIDDEN, "Admin access required");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.touch();
        userRepository.save(user);

        notificationService.create(user.getId(), "security", "Account unlocked", "An admin cleared your login lock and failed attempt counter.");
        auditLogService.log(admin.getId(), "admin_unlock_user", "success", "Unlocked user " + user.getEmail());

        return ResponseEntity.ok(ApiResponse.successMessage("User unlocked successfully"));
    }

    @PostMapping("/broadcast")
    @Transactional
    public ResponseEntity<Object> broadcast(Principal principal, @RequestBody Map<String, Object> request) {
        User admin = requireAdmin(principal);
        if (admin == null) {
            return error(HttpStatus.FORBIDDEN, "Admin access required");
        }

        String title = request != null && request.get("title") != null ? String.valueOf(request.get("title")).trim() : "";
        String message = request != null && request.get("message") != null ? String.valueOf(request.get("message")).trim() : "";
        if (title.isBlank() || message.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Title and message are required");
        }

        List<User> users = userRepository.findAll();
        for (User user : users) {
            notificationService.create(user.getId(), "broadcast", title, message);
        }
        auditLogService.log(admin.getId(), "admin_broadcast", "success", "Broadcast sent to " + users.size() + " users");

        Map<String, Object> response = ApiResponse.successMessage("Broadcast sent successfully");
        response.put("recipient_count", users.size());
        return ResponseEntity.ok(response);
    }

    private User requireAdmin(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            return null;
        }
        return user;
    }

    private Map<String, Object> userPayload(User user) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("username", user.getUsername());
        item.put("name", user.getName());
        item.put("email", user.getEmail());
        item.put("role", user.getRole());
        item.put("failed_login_attempts", user.getFailedLoginAttempts());
        item.put("locked_until", user.getLockedUntil() != null ? user.getLockedUntil().toString() : null);
        item.put("last_login_at", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        item.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        return item;
    }
}
