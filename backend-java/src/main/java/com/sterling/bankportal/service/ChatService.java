package com.sterling.bankportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.Beneficiary;
import com.sterling.bankportal.model.Notification;
import com.sterling.bankportal.model.SavingsGoal;
import com.sterling.bankportal.model.Transaction;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.BeneficiaryRepository;
import com.sterling.bankportal.repo.NotificationRepository;
import com.sterling.bankportal.repo.SavingsGoalRepository;
import com.sterling.bankportal.repo.TransactionRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ObjectMapper objectMapper;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final NotificationRepository notificationRepository;
    private final HttpClient httpClient;

    @Value("${app.openai.api-key:}")
    private String openAiApiKey;

    @Value("${app.openai.model:gpt-5-mini}")
    private String openAiModel;

    public ChatService(
            ObjectMapper objectMapper,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            BeneficiaryRepository beneficiaryRepository,
            SavingsGoalRepository savingsGoalRepository,
            NotificationRepository notificationRepository) {
        this.objectMapper = objectMapper;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.notificationRepository = notificationRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public Map<String, Object> reply(User user, String message, List<Map<String, String>> history) {
        String normalizedMessage = message == null ? "" : message.trim();
        String context = buildContext(user);
        String suggestedRoute = suggestRoute(normalizedMessage);
        String provider = "local";
        String warning = null;
        String answer;

        if (openAiApiKey != null && !openAiApiKey.isBlank()) {
            try {
                answer = callOpenAi(normalizedMessage, context, history);
                provider = "openai";
            } catch (Exception ex) {
                answer = localFallback(normalizedMessage, context, suggestedRoute);
                warning = "OpenAI is configured but unavailable right now, so the assistant used local banking guidance instead.";
            }
        } else {
            answer = localFallback(normalizedMessage, context, suggestedRoute);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", answer);
        response.put("provider", provider);
        response.put("model", provider.equals("openai") ? openAiModel : "local-banking-helper");
        response.put("suggested_route", suggestedRoute);
        response.put("warning", warning);
        response.put("mode", openAiApiKey != null && !openAiApiKey.isBlank() ? "api" : "local");
        return response;
    }

    private String callOpenAi(String message, String context, List<Map<String, String>> history) throws IOException, InterruptedException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiModel);
        payload.put("instructions", """
                You are Vibe Bank Assistant, an app-specific banking helper for a college banking project.
                Your scope is limited to:
                - Vibe Bank app usage
                - banking concepts related to payments, accounts, transactions, credit cards, beneficiaries, security, and savings goals
                - the signed-in user's provided app data context
                You must not help with unrelated topics, coding help, essays, or general chat outside banking/app support.
                Do not provide regulated financial or investment advice. You may give simple spending observations or savings suggestions.
                If the question is unrelated, politely refuse and redirect to Vibe Bank app or banking topics.
                Keep answers concise, practical, and student-demo friendly.
                """);

        List<Map<String, Object>> input = new ArrayList<>();
        input.add(inputMessage("user", "User app context:\n" + context));
        if (history != null) {
            for (Map<String, String> item : history) {
                String role = item.getOrDefault("role", "user");
                String content = item.getOrDefault("content", "");
                if (!content.isBlank() && ("user".equals(role) || "assistant".equals(role))) {
                    input.add(inputMessage(role, content));
                }
            }
        }
        input.add(inputMessage("user", message));
        payload.put("input", input);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("OpenAI API error: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String direct = root.path("output_text").asText("");
        if (!direct.isBlank()) {
            return direct.trim();
        }

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode part : content) {
                        String text = part.path("text").asText("");
                        if (!text.isBlank()) {
                            return text.trim();
                        }
                    }
                }
            }
        }
        throw new IOException("OpenAI response did not include assistant text");
    }

    private Map<String, Object> inputMessage(String role, String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        List<Map<String, String>> content = new ArrayList<>();
        Map<String, String> part = new LinkedHashMap<>();
        part.put("type", "input_text");
        part.put("text", text);
        content.add(part);
        message.put("content", content);
        return message;
    }

    private String buildContext(User user) {
        List<Account> accounts = accountRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        List<String> accountIds = accounts.stream().map(Account::getId).toList();
        List<Transaction> transactions = accountIds.isEmpty()
                ? List.of()
                : transactionRepository.findByAccountIdInOrderByCreatedAtDesc(accountIds).stream().limit(8).toList();
        List<Beneficiary> beneficiaries = beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().limit(5).toList();
        List<SavingsGoal> goals = savingsGoalRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().limit(5).toList();
        long unreadNotifications = notificationRepository.countByUserIdAndReadFalse(user.getId());
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().limit(3).toList();

        StringBuilder builder = new StringBuilder();
        builder.append("User: ").append(user.getName()).append(" (@").append(user.getUsername()).append(")\n");
        builder.append("Email: ").append(user.getEmail()).append("\n");
        builder.append("Accounts:\n");
        if (accounts.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (Account account : accounts) {
                builder.append("- ")
                        .append(account.getAccountType())
                        .append(" ")
                        .append(account.getAccountNumber())
                        .append(": balance Rs. ")
                        .append(String.format(Locale.US, "%.2f", account.getBalance()))
                        .append("\n");
            }
        }
        builder.append("Recent transactions:\n");
        if (transactions.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (Transaction transaction : transactions) {
                builder.append("- ")
                        .append(transaction.getEntryType())
                        .append(" Rs. ")
                        .append(String.format(Locale.US, "%.2f", transaction.getAmount()))
                        .append(" via ")
                        .append(transaction.getMethod())
                        .append(" for ")
                        .append(transaction.getDescription())
                        .append("\n");
            }
        }
        builder.append("Saved beneficiaries:\n");
        if (beneficiaries.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (Beneficiary beneficiary : beneficiaries) {
                builder.append("- ")
                        .append(beneficiary.getName())
                        .append(" (")
                        .append(beneficiary.getType())
                        .append(")\n");
            }
        }
        builder.append("Savings goals:\n");
        if (goals.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (SavingsGoal goal : goals) {
                builder.append("- ")
                        .append(goal.getTitle())
                        .append(": Rs. ")
                        .append(String.format(Locale.US, "%.2f", goal.getSavedAmount()))
                        .append(" / Rs. ")
                        .append(String.format(Locale.US, "%.2f", goal.getTargetAmount()))
                        .append(" (")
                        .append(goal.getStatus())
                        .append(")\n");
            }
        }
        builder.append("Unread notifications: ").append(unreadNotifications).append("\n");
        builder.append("Recent notifications:\n");
        if (notifications.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (Notification notification : notifications) {
                builder.append("- ").append(notification.getTitle()).append(": ").append(notification.getMessage()).append("\n");
            }
        }
        return builder.toString();
    }

    private String localFallback(String message, String context, String suggestedRoute) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("balance")) {
            return "Your balance details are available in the Dashboard or account selector. " + extractAccountLines(context);
        }
        if (lower.contains("transaction") || lower.contains("history")) {
            return "You can review recent transfers from the Transactions page. I can help explain credits, debits, filters, statement export, or goal contribution entries.";
        }
        if (lower.contains("beneficiar")) {
            return "Saved beneficiaries are managed from Payments. After a valid transfer, you can save the receiver and reuse that entry later from the same page.";
        }
        if (lower.contains("goal") || lower.contains("save money") || lower.contains("saving")) {
            return "Use Savings Goals to create a target, then contribute from a selected account. Contributions reduce that account balance and create a transaction log, which is good to demo for your DBMS project.";
        }
        if (lower.contains("credit card")) {
            return "Your credit card view shows holder name, masked number, expiry, credit limit, used limit, and available balance. It is mainly for project demonstration in this app.";
        }
        if (lower.contains("password") || lower.contains("pin") || lower.contains("login")) {
            return "For security tasks, use Profile for password and transaction PIN changes. Login supports username or email, failed attempts are tracked, and repeated failures can temporarily lock the account.";
        }
        if (lower.contains("payment") || lower.contains("transfer") || lower.contains("upi") || lower.contains("neft") || lower.contains("imps") || lower.contains("rtgs")) {
            return "Vibe Bank supports account transfer, UPI, IMPS, NEFT, and RTGS flows. Internal transfers update both sender and receiver ledgers. If you want, ask about a specific transfer type or open " + suggestedRoute + ".";
        }
        if (lower.contains("invest") || lower.contains("stock") || lower.contains("crypto")) {
            return "I can help with savings habits and spending patterns inside Vibe Bank, but I should not give investment advice. I can still summarize balances, transfers, and goals to help you plan saving better.";
        }
        if (lower.contains("hello") || lower.contains("hi")) {
            return "I’m your Vibe Bank assistant. Ask about balances, transactions, payments, beneficiaries, notifications, savings goals, card details, or how to use this app.";
        }
        return "I can help with Vibe Bank app usage, banking flows, and your in-app data like accounts, transactions, beneficiaries, goals, and alerts. Try asking about your balance, recent transfers, saving goals, beneficiaries, or how a payment method works.";
    }

    private String extractAccountLines(String context) {
        String[] lines = context.split("\n");
        List<String> accountLines = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("- ")) {
                accountLines.add(line);
            }
            if (accountLines.size() >= 2) {
                break;
            }
        }
        return accountLines.isEmpty() ? "" : "Current account snapshot: " + String.join(" ", accountLines);
    }

    private String suggestRoute(String message) {
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (lower.contains("goal") || lower.contains("saving")) {
            return "/goals";
        }
        if (lower.contains("transaction") || lower.contains("statement") || lower.contains("history")) {
            return "/transactions";
        }
        if (lower.contains("beneficiar") || lower.contains("payment") || lower.contains("transfer") || lower.contains("upi")) {
            return "/payments";
        }
        if (lower.contains("notification") || lower.contains("alert")) {
            return "/notifications";
        }
        if (lower.contains("password") || lower.contains("profile") || lower.contains("pin")) {
            return "/profile";
        }
        if (lower.contains("admin") || lower.contains("report")) {
            return "/admin";
        }
        return "/dashboard";
    }
}
