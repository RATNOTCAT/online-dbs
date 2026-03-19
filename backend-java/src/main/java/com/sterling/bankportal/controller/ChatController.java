package com.sterling.bankportal.controller;

import com.sterling.bankportal.model.ChatMessageRecord;
import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.SavingsGoal;
import com.sterling.bankportal.model.Transaction;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.ChatMessageRecordRepository;
import com.sterling.bankportal.repo.SavingsGoalRepository;
import com.sterling.bankportal.repo.TransactionRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.service.ChatService;
import com.sterling.bankportal.util.ApiResponse;
import java.security.Principal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController extends BaseController {

    private final UserRepository userRepository;
    private final ChatService chatService;
    private final ChatMessageRecordRepository chatMessageRecordRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    public ChatController(
            UserRepository userRepository,
            ChatService chatService,
            ChatMessageRecordRepository chatMessageRecordRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            SavingsGoalRepository savingsGoalRepository) {
        this.userRepository = userRepository;
        this.chatService = chatService;
        this.chatMessageRecordRepository = chatMessageRecordRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Object> status() {
        Map<String, Object> response = ApiResponse.success();
        response.put("status", "ready");
        response.put("message", "Vibe Bank assistant endpoint is available.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/insights")
    public ResponseEntity<Object> insights(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Account> accounts = accountRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        List<String> accountIds = accounts.stream().map(Account::getId).toList();
        List<Transaction> transactions = accountIds.isEmpty()
                ? List.of()
                : transactionRepository.findByAccountIdInOrderByCreatedAtDesc(accountIds);
        List<SavingsGoal> goals = savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        double totalBalance = accounts.stream().mapToDouble(Account::getBalance).sum();
        double totalCredits = transactions.stream().filter(item -> "credit".equalsIgnoreCase(item.getEntryType())).mapToDouble(Transaction::getAmount).sum();
        double totalDebits = transactions.stream().filter(item -> !"credit".equalsIgnoreCase(item.getEntryType())).mapToDouble(Transaction::getAmount).sum();
        String topMethod = transactions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> item.getMethod() == null || item.getMethod().isBlank() ? "other" : item.getMethod(),
                        java.util.stream.Collectors.summingDouble(Transaction::getAmount)))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");
        SavingsGoal nearestGoal = goals.stream()
                .filter(goal -> !"completed".equalsIgnoreCase(goal.getStatus()))
                .min(Comparator.comparingDouble(goal -> goal.getTargetAmount() - goal.getSavedAmount()))
                .orElse(null);

        List<String> insights = new java.util.ArrayList<>();
        insights.add("Total balance across your accounts is Rs. " + String.format("%.2f", totalBalance) + ".");
        insights.add("Credits recorded: Rs. " + String.format("%.2f", totalCredits) + "; debits recorded: Rs. " + String.format("%.2f", totalDebits) + ".");
        if (!"none".equals(topMethod)) {
            insights.add("Your most used transfer method by amount is " + topMethod.toUpperCase() + ".");
        }
        if (nearestGoal != null) {
            insights.add("Your closest active goal is \"" + nearestGoal.getTitle() + "\" with Rs. "
                    + String.format("%.2f", Math.max(0, nearestGoal.getTargetAmount() - nearestGoal.getSavedAmount()))
                    + " remaining.");
        }
        if (transactions.stream().limit(5).allMatch(item -> !"credit".equalsIgnoreCase(item.getEntryType())) && !transactions.isEmpty()) {
            insights.add("Your most recent activity is mostly debit-heavy. You may want to review recent transfers in Transactions.");
        }

        Map<String, Object> response = ApiResponse.success();
        response.put("insights", insights);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Object> history(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        List<Map<String, Object>> items = chatMessageRecordRepository.findTop30ByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(this::payload)
                .toList();
        Map<String, Object> response = ApiResponse.success();
        response.put("messages", items);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> chat(Principal principal, @RequestBody Map<String, Object> request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        String message = request != null && request.get("message") != null
                ? String.valueOf(request.get("message")).trim()
                : "";
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = request != null && request.get("history") instanceof List<?>
                ? (List<Map<String, String>>) request.get("history")
                : List.of();

        if (message.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Message is required");
        }

        Map<String, Object> chat = chatService.reply(user, message, history);
        chatMessageRecordRepository.save(record(user.getId(), "user", message, null, null));
        chatMessageRecordRepository.save(record(
                user.getId(),
                "assistant",
                String.valueOf(chat.getOrDefault("message", "")),
                (String) chat.get("provider"),
                (String) chat.get("suggested_route")));
        Map<String, Object> response = new LinkedHashMap<>(ApiResponse.success());
        response.putAll(chat);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/history")
    @Transactional
    public ResponseEntity<Object> clearHistory(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        chatMessageRecordRepository.deleteByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.successMessage("Assistant history cleared"));
    }

    private ChatMessageRecord record(String userId, String role, String content, String provider, String suggestedRoute) {
        ChatMessageRecord item = new ChatMessageRecord();
        item.setUserId(userId);
        item.setRole(role);
        item.setContent(content);
        item.setProvider(provider);
        item.setSuggestedRoute(suggestedRoute);
        return item;
    }

    private Map<String, Object> payload(ChatMessageRecord item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", item.getId());
        data.put("role", item.getRole());
        data.put("content", item.getContent());
        data.put("provider", item.getProvider());
        data.put("suggested_route", item.getSuggestedRoute());
        data.put("created_at", item.getCreatedAt() != null ? item.getCreatedAt().toString() : null);
        return data;
    }
}
