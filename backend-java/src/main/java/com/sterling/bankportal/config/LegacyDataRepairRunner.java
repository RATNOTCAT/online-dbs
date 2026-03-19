package com.sterling.bankportal.config;

import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.Transaction;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.TransactionRepository;
import com.sterling.bankportal.repo.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyDataRepairRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyDataRepairRunner.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Value("${app.admin.email:admin@vibebank.com}")
    private String adminEmail;

    public LegacyDataRepairRunner(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        repairUsers();
        repairTransactions();
    }

    private void repairUsers() {
        List<User> users = userRepository.findAll();
        Set<String> usedUsernames = new LinkedHashSet<>();
        for (User user : users) {
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                usedUsernames.add(user.getUsername().toLowerCase(Locale.ROOT));
            }
        }

        List<User> changedUsers = new ArrayList<>();
        for (User user : users) {
            boolean dirty = false;
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                user.setUsername(generateUsername(user, usedUsernames));
                dirty = true;
            }

            String expectedRole = isAdminEmail(user.getEmail()) ? "admin" : "user";
            if (!expectedRole.equalsIgnoreCase(user.getRole()) && "user".equalsIgnoreCase(user.getRole())) {
                user.setRole(expectedRole);
                dirty = true;
            }

            if (dirty) {
                changedUsers.add(user);
            }
        }

        if (!changedUsers.isEmpty()) {
            userRepository.saveAll(changedUsers);
            log.info("Backfilled {} legacy user record(s) for Spring Boot compatibility", changedUsers.size());
        }
    }

    private void repairTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        if (transactions.isEmpty()) {
            return;
        }

        Map<String, User> usersById = new LinkedHashMap<>();
        for (User user : userRepository.findAll()) {
            usersById.put(user.getId(), user);
        }

        Map<String, Account> accountsById = new LinkedHashMap<>();
        Map<String, Account> firstAccountByUserId = new LinkedHashMap<>();
        for (Account account : accountRepository.findAll()) {
            accountsById.put(account.getId(), account);
            firstAccountByUserId.putIfAbsent(account.getUserId(), account);
        }

        List<Transaction> changedTransactions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            boolean dirty = false;

            if (transaction.getEntryType() == null || transaction.getEntryType().isBlank()) {
                transaction.setEntryType("debit");
                dirty = true;
            }

            if ((transaction.getSenderName() == null || transaction.getSenderName().isBlank()) && transaction.getSenderId() != null) {
                User sender = usersById.get(transaction.getSenderId());
                if (sender != null && sender.getName() != null && !sender.getName().isBlank()) {
                    transaction.setSenderName(sender.getName());
                    dirty = true;
                }
            }

            if ((transaction.getSenderAccountNo() == null || transaction.getSenderAccountNo().isBlank()) && transaction.getSenderId() != null) {
                Account senderAccount = accountsById.get(transaction.getAccountId());
                if (senderAccount == null || !transaction.getSenderId().equals(senderAccount.getUserId())) {
                    senderAccount = firstAccountByUserId.get(transaction.getSenderId());
                }
                if (senderAccount != null && senderAccount.getAccountNumber() != null && !senderAccount.getAccountNumber().isBlank()) {
                    transaction.setSenderAccountNo(senderAccount.getAccountNumber());
                    dirty = true;
                }
            }

            if ((transaction.getReceiverName() == null || transaction.getReceiverName().isBlank()) && transaction.getReceiverId() != null) {
                User receiver = usersById.get(transaction.getReceiverId());
                if (receiver != null && receiver.getName() != null && !receiver.getName().isBlank()) {
                    transaction.setReceiverName(receiver.getName());
                    dirty = true;
                }
            }

            if ((transaction.getReceiverAccountNo() == null || transaction.getReceiverAccountNo().isBlank()) && transaction.getReceiverId() != null) {
                Account receiverAccount = firstAccountByUserId.get(transaction.getReceiverId());
                if (receiverAccount != null && receiverAccount.getAccountNumber() != null && !receiverAccount.getAccountNumber().isBlank()) {
                    transaction.setReceiverAccountNo(receiverAccount.getAccountNumber());
                    dirty = true;
                }
            }

            if (dirty) {
                changedTransactions.add(transaction);
            }
        }

        if (!changedTransactions.isEmpty()) {
            transactionRepository.saveAll(changedTransactions);
            log.info("Backfilled {} legacy transaction record(s) for Spring Boot compatibility", changedTransactions.size());
        }
    }

    private String generateUsername(User user, Set<String> usedUsernames) {
        String base = user.getEmail() != null && user.getEmail().contains("@")
                ? user.getEmail().substring(0, user.getEmail().indexOf('@'))
                : user.getName();
        if (base == null || base.isBlank()) {
            base = "user";
        }

        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (base.length() < 3) {
            base = (base + "user").substring(0, 4);
        }
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }

        String candidate = base;
        int suffix = 1;
        while (usedUsernames.contains(candidate)) {
            candidate = base;
            String tail = String.valueOf(suffix++);
            int maxBaseLength = Math.max(3, 24 - tail.length());
            if (candidate.length() > maxBaseLength) {
                candidate = candidate.substring(0, maxBaseLength);
            }
            candidate = candidate + tail;
        }
        usedUsernames.add(candidate);
        return candidate;
    }

    private boolean isAdminEmail(String email) {
        return email != null && adminEmail != null && email.equalsIgnoreCase(adminEmail);
    }
}
