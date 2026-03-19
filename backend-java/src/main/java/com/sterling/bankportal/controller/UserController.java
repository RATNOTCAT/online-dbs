package com.sterling.bankportal.controller;

import com.sterling.bankportal.dto.UserRequests.ChangePasswordRequest;
import com.sterling.bankportal.dto.UserRequests.SetTransactionPinRequest;
import com.sterling.bankportal.dto.UserRequests.UpdateProfileRequest;
import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.Notification;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.AuditLogRepository;
import com.sterling.bankportal.repo.BeneficiaryRepository;
import com.sterling.bankportal.repo.NotificationRepository;
import com.sterling.bankportal.repo.SavingsGoalRepository;
import com.sterling.bankportal.repo.TransactionRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.service.AuditLogService;
import com.sterling.bankportal.service.NotificationService;
import com.sterling.bankportal.util.ApiResponse;
import com.sterling.bankportal.util.ResponseMapper;
import com.sterling.bankportal.util.Validators;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public UserController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            BeneficiaryRepository beneficiaryRepository,
            AuditLogRepository auditLogRepository,
            NotificationRepository notificationRepository,
            SavingsGoalRepository savingsGoalRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/profile")
    public ResponseEntity<Object> getProfile(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        Account account = requireAccount(user.getId(), accountRepository);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.getId());
        payload.put("username", user.getUsername());
        payload.put("name", user.getName());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole());
        payload.put("phone", user.getPhone());
        payload.put("address", user.getAddress());
        payload.put("date_of_birth", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
        payload.put("aadhar_number", user.getAadharNumber());
        payload.put("pan_number", user.getPanNumber());
        payload.put("joined_date", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        payload.put("account_number", account != null ? account.getAccountNumber() : null);
        payload.put("account_type", account != null ? account.getAccountType() : null);

        Map<String, Object> response = ApiResponse.success();
        response.put("user", payload);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Transactional
    public ResponseEntity<Object> updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        if (request.getName() != null) {
            String nameMessage = Validators.validateAccountHolderName(request.getName());
            if (nameMessage != null) {
                return error(HttpStatus.BAD_REQUEST, nameMessage);
            }
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            if (!Validators.isValidPhone(request.getPhone())) {
                return error(HttpStatus.BAD_REQUEST, "Invalid phone number");
            }
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getDate_of_birth() != null) {
            try {
                user.setDateOfBirth(LocalDate.parse(request.getDate_of_birth()));
            } catch (Exception ex) {
                return error(HttpStatus.BAD_REQUEST, "Invalid date format");
            }
        }
        if (request.getAadhar_number() != null) {
            if (!request.getAadhar_number().matches("\\d{12}")) {
                return error(HttpStatus.BAD_REQUEST, "Invalid Aadhar number");
            }
            user.setAadharNumber(request.getAadhar_number());
        }
        if (request.getPan_number() != null) {
            if (request.getPan_number().length() != 10) {
                return error(HttpStatus.BAD_REQUEST, "Invalid PAN number");
            }
            user.setPanNumber(request.getPan_number());
        }

        user.touch();
        userRepository.save(user);
        auditLogService.log(user.getId(), "profile_update", "success", "Updated profile details");

        Map<String, Object> response = ApiResponse.successMessage("Profile updated successfully");
        response.put("user", ResponseMapper.user(user));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<Object> changePassword(Principal principal, @RequestBody ChangePasswordRequest request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        if (request == null || request.getOld_password() == null || request.getNew_password() == null) {
            return error(HttpStatus.BAD_REQUEST, "Missing required fields");
        }

        if (!passwordEncoder.matches(request.getOld_password(), user.getPasswordHash())) {
            return error(HttpStatus.UNAUTHORIZED, "Old password is incorrect");
        }

        String passwordMessage = Validators.validatePassword(request.getNew_password());
        if (passwordMessage != null) {
            return error(HttpStatus.BAD_REQUEST, passwordMessage);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNew_password()));
        user.touch();
        userRepository.save(user);
        auditLogService.log(user.getId(), "password_change", "success", "Changed account password");
        return ResponseEntity.ok(ApiResponse.successMessage("Password changed successfully"));
    }

    @PostMapping("/set-transaction-pin")
    @Transactional
    public ResponseEntity<Object> setTransactionPin(Principal principal, @RequestBody SetTransactionPinRequest request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        if (request == null || request.getPin() == null || request.getPassword() == null) {
            return error(HttpStatus.BAD_REQUEST, "Missing PIN or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return error(HttpStatus.UNAUTHORIZED, "Password is incorrect");
        }

        if (!Validators.isValidTransactionPin(request.getPin())) {
            return error(HttpStatus.BAD_REQUEST, "PIN must be 4 digits");
        }

        user.setTransactionPin(request.getPin());
        user.touch();
        userRepository.save(user);
        auditLogService.log(user.getId(), "pin_update", "success", "Updated transaction PIN");
        return ResponseEntity.ok(ApiResponse.successMessage("Transaction PIN set successfully"));
    }

    @GetMapping("/admin-summary")
    public ResponseEntity<Object> getAdminSummary(Principal principal) {
        User admin = requireAdminUser(principal);
        if (admin == null) {
            return error(HttpStatus.FORBIDDEN, "Admin access required");
        }

        var transactions = transactionRepository.findAll();
        List<Notification> notifications = notificationRepository.findAll();
        var goals = savingsGoalRepository.findAll();

        Map<String, Object> summary = new LinkedHashMap<>();
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
        summary.put("locked_users", userRepository.findAll().stream()
                .filter(user -> user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now()))
                .count());
        summary.put("recent_audits", auditLogRepository.findTop20ByOrderByCreatedAtDesc());
        summary.put("recent_notifications", notifications.stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .limit(10)
                .map(notification -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", notification.getId());
                    item.put("title", notification.getTitle());
                    item.put("type", notification.getType());
                    item.put("is_read", notification.isRead());
                    item.put("created_at", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null);
                    return item;
                })
                .toList());
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
        summary.put("users", userRepository.findAll().stream().map(this::adminUserPayload).toList());

        Map<String, Object> response = ApiResponse.success();
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/users/{userId}/unlock")
    @Transactional
    public ResponseEntity<Object> unlockUser(Principal principal, @PathVariable String userId) {
        User admin = requireAdminUser(principal);
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

    @PostMapping("/admin/broadcast")
    @Transactional
    public ResponseEntity<Object> sendBroadcast(Principal principal, @RequestBody Map<String, Object> request) {
        User admin = requireAdminUser(principal);
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

    private User requireAdminUser(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
            return null;
        }
        return user;
    }

    private Map<String, Object> adminUserPayload(User user) {
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
