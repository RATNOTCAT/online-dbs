package com.sterling.bankportal.controller;

import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.service.AuditLogService;
import com.sterling.bankportal.service.NotificationService;
import com.sterling.bankportal.util.ApiResponse;
import com.sterling.bankportal.util.ResponseMapper;
import java.security.Principal;
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
@RequestMapping("/api")
public class AccountController extends BaseController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final Random random = new Random();

    public AccountController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            NotificationService notificationService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/account")
    public ResponseEntity<Object> getAccount(
            Principal principal,
            @RequestParam(name = "account_number", required = false) String accountNumber) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        Account account = resolveOwnedAccount(user.getId(), accountNumber);
        if (account == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }

        Map<String, Object> response = ApiResponse.success();
        response.put("account", ResponseMapper.account(account));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/balance")
    public ResponseEntity<Object> getBalance(
            Principal principal,
            @RequestParam(name = "account_number", required = false) String accountNumber) {
        Account account = resolveOwnedAccount(userId(principal), accountNumber);
        if (account == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }

        Map<String, Object> response = ApiResponse.success();
        response.put("balance", account.getBalance());
        response.put("account_number", account.getAccountNumber());
        response.put("account_type", account.getAccountType());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts")
    public ResponseEntity<Object> getAccounts(Principal principal) {
        List<Map<String, Object>> accounts = accountRepository.findByUserIdOrderByCreatedAtAsc(userId(principal))
                .stream()
                .map(ResponseMapper::account)
                .toList();
        Map<String, Object> response = ApiResponse.success();
        response.put("accounts", accounts);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts")
    @Transactional
    public ResponseEntity<Object> createAccount(Principal principal, @RequestBody Map<String, Object> request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        String accountType = request != null && request.get("account_type") != null
                ? String.valueOf(request.get("account_type")).trim()
                : "Savings";
        if (accountType.isBlank()) {
            accountType = "Savings";
        }

        Account account = new Account();
        account.setUserId(user.getId());
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountType(accountType);
        account.setBalance(0.0);
        accountRepository.save(account);
        notificationService.create(user.getId(), "account", "New account created", accountType + " account " + account.getAccountNumber() + " was created.");
        auditLogService.log(user.getId(), "account_create", "success", "Created " + accountType + " account " + account.getAccountNumber());

        Map<String, Object> response = ApiResponse.successMessage("Account created successfully");
        response.put("account", ResponseMapper.account(account));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/account/lookup")
    public ResponseEntity<Object> lookup(@RequestParam(name = "account_number", required = false) String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "account_number query param required");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber.trim()).orElse(null);
        if (account == null) {
            return error(HttpStatus.NOT_FOUND, "Account not found");
        }

        User owner = userRepository.findById(account.getUserId()).orElse(null);
        Map<String, Object> response = ApiResponse.success();
        response.put("account", ResponseMapper.account(account));
        response.put("owner_name", owner != null ? owner.getName() : null);
        return ResponseEntity.ok(response);
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

    private String generateUniqueAccountNumber() {
        String value;
        do {
            StringBuilder builder = new StringBuilder("4082");
            for (int i = 0; i < 12; i++) {
                builder.append(random.nextInt(10));
            }
            value = builder.toString();
        } while (accountRepository.existsByAccountNumber(value));
        return value;
    }
}
