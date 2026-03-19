package com.sterling.bankportal.controller;

import com.sterling.bankportal.dto.TransactionRequests.AccountTransferRequest;
import com.sterling.bankportal.dto.TransactionRequests.SimpleTransferRequest;
import com.sterling.bankportal.dto.TransactionRequests.UpiTransferRequest;
import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.Transaction;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.TransactionRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.service.AuditLogService;
import com.sterling.bankportal.service.NotificationService;
import com.sterling.bankportal.util.ApiResponse;
import com.sterling.bankportal.util.ResponseMapper;
import com.sterling.bankportal.util.Validators;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController extends BaseController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final Random random = new Random();

    public TransactionController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            NotificationService notificationService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Object> getTransactions(
            Principal principal,
            @RequestParam(name = "account_number", required = false) String accountNumber) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Account> accounts = accountNumber != null && !accountNumber.trim().isEmpty()
                ? resolveOwnedAccount(user.getId(), accountNumber) != null
                    ? List.of(resolveOwnedAccount(user.getId(), accountNumber))
                    : List.of()
                : accountRepository.findByUserIdOrderByCreatedAtAsc(user.getId());

        if (accounts.isEmpty() || accounts.get(0) == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }

        List<String> accountIds = accounts.stream().map(Account::getId).toList();
        List<Map<String, Object>> transactions = transactionRepository.findByAccountIdInOrderByCreatedAtDesc(accountIds)
                .stream()
                .map(ResponseMapper::transaction)
                .toList();

        Map<String, Object> response = ApiResponse.success();
        response.put("transactions", transactions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/simple-transfer")
    @Transactional
    public ResponseEntity<Object> simpleTransfer(Principal principal, @RequestBody SimpleTransferRequest request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        if (request == null || request.getReceiver_account() == null || request.getAmount() == null || request.getDescription() == null) {
            return error(HttpStatus.BAD_REQUEST, "Missing required fields");
        }
        if (!Validators.isPositiveAmount(request.getAmount())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid amount");
        }
        if (!Validators.isValidAccountNumber(request.getReceiver_account().trim())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid account number");
        }

        Account sender = resolveOwnedAccount(user.getId(), request.getSource_account());
        if (sender == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }
        if (sender.getBalance() < request.getAmount()) {
            return error(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        Account receiver = accountRepository.findByAccountNumber(request.getReceiver_account().trim()).orElse(null);
        if (receiver == null) {
            return error(HttpStatus.NOT_FOUND, "Receiver account not found");
        }
        if (receiver.getId().equals(sender.getId())) {
            return error(HttpStatus.BAD_REQUEST, "Sender and receiver accounts cannot be the same");
        }

        User receiverUser = userRepository.findById(receiver.getUserId()).orElse(null);
        postInternalTransfer(
                sender,
                user,
                receiver,
                receiverUser,
                request.getAmount(),
                request.getDescription(),
                "transfer",
                "account");

        Transaction senderLedger = latestAccountTransaction(sender.getId());
        Map<String, Object> response = ApiResponse.successMessage("Transfer completed successfully");
        response.put("transaction", ResponseMapper.transaction(senderLedger));
        response.put("new_balance", sender.getBalance());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/account-transfer")
    @Transactional
    public ResponseEntity<Object> accountTransfer(Principal principal, @RequestBody AccountTransferRequest request) {
        return accountBasedTransfer(principal, request, "transfer", "account", "Transfer completed successfully", 0);
    }

    @PostMapping("/imps")
    @Transactional
    public ResponseEntity<Object> impsTransfer(Principal principal, @RequestBody AccountTransferRequest request) {
        return accountBasedTransfer(principal, request, "imps", "imps", "IMPS transfer completed successfully", 0);
    }

    @PostMapping("/neft")
    @Transactional
    public ResponseEntity<Object> neftTransfer(Principal principal, @RequestBody AccountTransferRequest request) {
        return accountBasedTransfer(principal, request, "neft", "neft", "NEFT transfer completed successfully", 0);
    }

    @PostMapping("/rtgs")
    @Transactional
    public ResponseEntity<Object> rtgsTransfer(Principal principal, @RequestBody AccountTransferRequest request) {
        return accountBasedTransfer(principal, request, "rtgs", "rtgs", "RTGS transfer completed successfully", 100000);
    }

    @PostMapping("/upi-transfer")
    @Transactional
    public ResponseEntity<Object> upiTransfer(Principal principal, @RequestBody UpiTransferRequest request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        if (request == null
                || request.getReceiver_upi() == null
                || request.getReceiver_name() == null
                || request.getAmount() == null
                || request.getDescription() == null) {
            return error(HttpStatus.BAD_REQUEST, "Missing required fields");
        }
        if (!Validators.isPositiveAmount(request.getAmount())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid amount");
        }
        if (!Validators.isValidUpi(request.getReceiver_upi())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid UPI ID");
        }

        Account account = resolveOwnedAccount(user.getId(), request.getSource_account());
        if (account == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }
        if (account.getBalance() < request.getAmount()) {
            return error(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        account.setBalance(account.getBalance() - request.getAmount());
        account.touch();
        accountRepository.save(account);

        Transaction transaction = createTransaction(
                account,
                user.getId(),
                null,
                "debit",
                "upi",
                "upi",
                request.getAmount(),
                request.getDescription(),
                user.getName(),
                account.getAccountNumber(),
                request.getReceiver_name(),
                null,
                null,
                request.getReceiver_upi());
        transactionRepository.save(transaction);
        notificationService.create(user.getId(), "transfer", "UPI transfer completed", "Sent Rs. " + request.getAmount() + " to " + request.getReceiver_name() + ".");
        auditLogService.log(user.getId(), "transfer_upi", "success", "UPI transfer to " + request.getReceiver_name());

        Map<String, Object> response = ApiResponse.successMessage("UPI transfer completed successfully");
        response.put("transaction", ResponseMapper.transaction(transaction));
        response.put("new_balance", account.getBalance());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Object> accountBasedTransfer(
            Principal principal,
            AccountTransferRequest request,
            String type,
            String method,
            String successMessage,
            double minimumAmount) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        if (request == null
                || request.getReceiver_account() == null
                || request.getReceiver_ifsc() == null
                || request.getReceiver_name() == null
                || request.getAmount() == null
                || request.getDescription() == null) {
            return error(HttpStatus.BAD_REQUEST, "Missing required fields");
        }
        if (!Validators.isPositiveAmount(request.getAmount())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid amount");
        }
        if (!Validators.isValidAccountNumber(request.getReceiver_account().trim())) {
            return error(HttpStatus.BAD_REQUEST, "Invalid account number");
        }
        if (minimumAmount > 0 && request.getAmount() < minimumAmount) {
            return error(HttpStatus.BAD_REQUEST, "RTGS minimum transfer amount is Rs. 1,00,000");
        }

        Account sender = resolveOwnedAccount(user.getId(), request.getSource_account());
        if (sender == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }
        if (sender.getBalance() < request.getAmount()) {
            return error(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        Account receiver = accountRepository.findByAccountNumber(request.getReceiver_account().trim()).orElse(null);
        if (receiver != null) {
            if (receiver.getId().equals(sender.getId())) {
                return error(HttpStatus.BAD_REQUEST, "Sender and receiver accounts cannot be the same");
            }
            User receiverUser = userRepository.findById(receiver.getUserId()).orElse(null);
            postInternalTransfer(sender, user, receiver, receiverUser, request.getAmount(), request.getDescription(), type, method);
        } else {
            sender.setBalance(sender.getBalance() - request.getAmount());
            sender.touch();
            accountRepository.save(sender);

            Transaction senderLedger = createTransaction(
                    sender,
                    user.getId(),
                    null,
                    "debit",
                    type,
                    method,
                    request.getAmount(),
                    request.getDescription(),
                    user.getName(),
                    sender.getAccountNumber(),
                    request.getReceiver_name(),
                    request.getReceiver_account(),
                    request.getReceiver_ifsc(),
                    null);
            transactionRepository.save(senderLedger);
            notificationService.create(user.getId(), "transfer", successMessage, "Sent Rs. " + request.getAmount() + " to " + request.getReceiver_name() + ".");
            auditLogService.log(user.getId(), "transfer_" + type, "success", "Transfer to external account " + request.getReceiver_account());
        }

        Transaction senderLedger = latestAccountTransaction(sender.getId());
        Map<String, Object> response = ApiResponse.successMessage(successMessage);
        response.put("transaction", ResponseMapper.transaction(senderLedger));
        response.put("new_balance", sender.getBalance());
        return ResponseEntity.ok(response);
    }

    private void postInternalTransfer(
            Account sender,
            User senderUser,
            Account receiver,
            User receiverUser,
            double amount,
            String description,
            String type,
            String method) {
        sender.setBalance(sender.getBalance() - amount);
        sender.touch();
        receiver.setBalance(receiver.getBalance() + amount);
        receiver.touch();
        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction senderLedger = createTransaction(
                sender,
                senderUser.getId(),
                receiver.getUserId(),
                "debit",
                type,
                method,
                amount,
                description,
                senderUser.getName(),
                sender.getAccountNumber(),
                receiverUser != null ? receiverUser.getName() : null,
                receiver.getAccountNumber(),
                receiver.getIfscCode(),
                null);

        Transaction receiverLedger = createTransaction(
                receiver,
                senderUser.getId(),
                receiver.getUserId(),
                "credit",
                type,
                method,
                amount,
                description,
                senderUser.getName(),
                sender.getAccountNumber(),
                receiverUser != null ? receiverUser.getName() : null,
                receiver.getAccountNumber(),
                receiver.getIfscCode(),
                null);

        transactionRepository.save(senderLedger);
        transactionRepository.save(receiverLedger);
        notificationService.create(senderUser.getId(), "transfer", "Transfer completed", "Sent Rs. " + amount + " to " + (receiverUser != null ? receiverUser.getName() : receiver.getAccountNumber()) + ".");
        if (receiverUser != null) {
            notificationService.create(receiverUser.getId(), "credit", "Money received", "Received Rs. " + amount + " from " + senderUser.getName() + ".");
        }
        auditLogService.log(senderUser.getId(), "transfer_" + type, "success", "Transferred Rs. " + amount + " to " + receiver.getAccountNumber());
        if (receiverUser != null) {
            auditLogService.log(receiverUser.getId(), "credit_" + type, "success", "Received Rs. " + amount + " from " + sender.getAccountNumber());
        }
    }

    private Transaction createTransaction(
            Account account,
            String senderId,
            String receiverId,
            String entryType,
            String type,
            String method,
            double amount,
            String description,
            String senderName,
            String senderAccount,
            String receiverName,
            String receiverAccount,
            String receiverIfsc,
            String receiverUpi) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(account.getId());
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setEntryType(entryType);
        transaction.setType(type);
        transaction.setMethod(method);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setSenderName(senderName);
        transaction.setSenderAccountNo(senderAccount);
        transaction.setReceiverName(receiverName);
        transaction.setReceiverAccountNo(receiverAccount);
        transaction.setReceiverIfsc(receiverIfsc);
        transaction.setReceiverUpi(receiverUpi);
        transaction.setStatus("completed");
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setReferenceNumber(generateReferenceNumber());
        return transaction;
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

    private Transaction latestAccountTransaction(String accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream().findFirst().orElse(null);
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
}
