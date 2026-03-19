package com.sterling.bankportal.controller;

import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.SavingsGoal;
import com.sterling.bankportal.model.Transaction;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController extends BaseController {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final Random random = new Random();

    public SavingsGoalController(
            SavingsGoalRepository savingsGoalRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            NotificationService notificationService,
            AuditLogService auditLogService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Object> list(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Map<String, Object>> goals = savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(ResponseMapper::goal)
                .toList();
        Map<String, Object> response = ApiResponse.success();
        response.put("goals", goals);
        response.put("summary", summary(goals));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> create(Principal principal, @RequestBody Map<String, Object> request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        String title = stringValue(request, "title");
        String description = stringValue(request, "description");
        String category = stringValue(request, "category");
        Double targetAmount = doubleValue(request, "target_amount");
        String targetDateValue = stringValue(request, "target_date");

        if (title == null || title.isBlank() || targetAmount == null) {
            return error(HttpStatus.BAD_REQUEST, "Title and target amount are required");
        }
        if (!Validators.isPositiveAmount(targetAmount)) {
            return error(HttpStatus.BAD_REQUEST, "Invalid target amount");
        }

        SavingsGoal goal = new SavingsGoal();
        goal.setUserId(user.getId());
        goal.setTitle(title.trim());
        goal.setDescription(description);
        goal.setCategory(category == null || category.isBlank() ? "General" : category.trim());
        goal.setTargetAmount(targetAmount);
        goal.setSavedAmount(0.0);
        if (targetDateValue != null && !targetDateValue.isBlank()) {
            goal.setTargetDate(LocalDate.parse(targetDateValue));
        }

        savingsGoalRepository.save(goal);
        notificationService.create(user.getId(), "goal", "Savings goal created", "Goal \"" + goal.getTitle() + "\" was created.");
        auditLogService.log(user.getId(), "goal_create", "success", "Created savings goal " + goal.getTitle());

        Map<String, Object> response = ApiResponse.successMessage("Savings goal created successfully");
        response.put("goal", ResponseMapper.goal(goal));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{goalId}/contribute")
    @Transactional
    public ResponseEntity<Object> contribute(Principal principal, @PathVariable String goalId, @RequestBody Map<String, Object> request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(goalId, user.getId()).orElse(null);
        if (goal == null) {
            return error(HttpStatus.NOT_FOUND, "Savings goal not found");
        }
        if ("completed".equalsIgnoreCase(goal.getStatus())) {
            return error(HttpStatus.BAD_REQUEST, "This goal is already completed");
        }

        Double amount = doubleValue(request, "amount");
        String accountNumber = stringValue(request, "account_number");
        if (amount == null || !Validators.isPositiveAmount(amount)) {
            return error(HttpStatus.BAD_REQUEST, "Invalid contribution amount");
        }

        Account account = resolveOwnedAccount(user.getId(), accountNumber);
        if (account == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }
        if (account.getBalance() < amount) {
            return error(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        account.setBalance(account.getBalance() - amount);
        account.touch();
        accountRepository.save(account);

        goal.setSavedAmount(goal.getSavedAmount() + amount);
        if (goal.getSavedAmount() >= goal.getTargetAmount()) {
            goal.setSavedAmount(goal.getTargetAmount());
            goal.setStatus("completed");
        } else {
            goal.setStatus("active");
        }
        goal.touch();
        savingsGoalRepository.save(goal);

        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setSenderId(user.getId());
        transaction.setReceiverId(null);
        transaction.setEntryType("debit");
        transaction.setType("goal_contribution");
        transaction.setMethod("goal");
        transaction.setAmount(amount);
        transaction.setDescription("Contribution to savings goal: " + goal.getTitle());
        transaction.setSenderName(user.getName());
        transaction.setSenderAccountNo(account.getAccountNumber());
        transaction.setReceiverName(goal.getTitle());
        transaction.setReceiverAccountNo(null);
        transaction.setReceiverIfsc(null);
        transaction.setReceiverUpi(null);
        transaction.setStatus("completed");
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setReferenceNumber(generateReferenceNumber());
        transactionRepository.save(transaction);

        String message = "Added Rs. " + amount + " to goal \"" + goal.getTitle() + "\".";
        if ("completed".equalsIgnoreCase(goal.getStatus())) {
            message = "Goal \"" + goal.getTitle() + "\" completed successfully.";
        }
        notificationService.create(user.getId(), "goal", "Savings goal updated", message);
        auditLogService.log(user.getId(), "goal_contribute", "success", "Contributed Rs. " + amount + " to goal " + goal.getTitle());

        Map<String, Object> response = ApiResponse.successMessage("Contribution added successfully");
        response.put("goal", ResponseMapper.goal(goal));
        response.put("transaction", ResponseMapper.transaction(transaction));
        response.put("new_balance", account.getBalance());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> summary(List<Map<String, Object>> goals) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_goals", goals.size());
        summary.put("completed_goals", goals.stream().filter(goal -> "completed".equals(goal.get("status"))).count());
        summary.put(
                "saved_amount",
                goals.stream().map(goal -> (Number) goal.get("saved_amount")).filter(value -> value != null).mapToDouble(Number::doubleValue).sum());
        summary.put(
                "target_amount",
                goals.stream().map(goal -> (Number) goal.get("target_amount")).filter(value -> value != null).mapToDouble(Number::doubleValue).sum());
        return summary;
    }

    private Account resolveOwnedAccount(String userId, String accountNumber) {
        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            Account account = accountRepository.findByAccountNumber(accountNumber.trim()).orElse(null);
            if (account != null && userId.equals(account.getUserId())) {
                return account;
            }
            return null;
        }
        return accountRepository.findFirstByUserId(userId).orElse(null);
    }

    private String generateReferenceNumber() {
        String candidate;
        do {
            candidate = "TXN"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + randomAlphaNumeric(6);
        } while (transactionRepository.existsByReferenceNumber(candidate));
        return candidate;
    }

    private String randomAlphaNumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    private String stringValue(Map<String, Object> request, String key) {
        if (request == null || request.get(key) == null) {
            return null;
        }
        return String.valueOf(request.get(key)).trim();
    }

    private Double doubleValue(Map<String, Object> request, String key) {
        if (request == null || request.get(key) == null) {
            return null;
        }
        Object value = request.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
