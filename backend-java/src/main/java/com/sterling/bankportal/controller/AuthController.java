package com.sterling.bankportal.controller;

import com.sterling.bankportal.dto.AuthRequests.LoginRequest;
import com.sterling.bankportal.dto.AuthRequests.RegisterRequest;
import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.CreditCard;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.CreditCardRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.security.JwtService;
import com.sterling.bankportal.service.AuditLogService;
import com.sterling.bankportal.service.NotificationService;
import com.sterling.bankportal.util.ApiResponse;
import com.sterling.bankportal.util.Validators;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController extends BaseController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final Random random = new Random();
    @org.springframework.beans.factory.annotation.Value("${app.admin.email:admin@vibebank.com}")
    private String adminEmail;

    public AuthController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            CreditCardRepository creditCardRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NotificationService notificationService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.creditCardRepository = creditCardRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Object> register(@RequestBody RegisterRequest request) {
        if (request == null
                || isBlank(request.getEmail())
                || isBlank(request.getPassword())
                || isBlank(request.getName())
                || isBlank(request.getUsername())) {
            return error(HttpStatus.BAD_REQUEST, "Missing required fields");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String displayName = request.getName().trim();
        String username = request.getUsername().trim();
        if (!Validators.isValidEmail(email)) {
            return error(HttpStatus.BAD_REQUEST, "Invalid email format");
        }
        String emailDomainMessage = Validators.validateEmailDomain(email);
        if (emailDomainMessage != null) {
            return error(HttpStatus.BAD_REQUEST, emailDomainMessage);
        }

        String usernameMessage = Validators.validateUsername(username);
        if (usernameMessage != null) {
            return error(HttpStatus.BAD_REQUEST, usernameMessage);
        }
        String nameMessage = Validators.validateAccountHolderName(displayName);
        if (nameMessage != null) {
            return error(HttpStatus.BAD_REQUEST, nameMessage);
        }
        if (username.equalsIgnoreCase(displayName) || username.equalsIgnoreCase(displayName.replace(" ", ""))) {
            return error(HttpStatus.BAD_REQUEST, "Username must not be the same as the account holder name");
        }

        if (userRepository.existsByEmail(email)) {
            return error(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return error(HttpStatus.CONFLICT, "Username already taken");
        }

        String passwordMessage = Validators.validatePassword(request.getPassword());
        if (passwordMessage != null) {
            return error(HttpStatus.BAD_REQUEST, passwordMessage);
        }

        User user = new User();
        user.setName(displayName);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(resolveBootstrapRole(email));
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        Account account = new Account();
        account.setUserId(user.getId());
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setAccountType("Savings");
        account.setBalance(100000.0);
        accountRepository.save(account);

        CreditCard card = new CreditCard();
        card.setUserId(user.getId());
        card.setCardNumber(generateUniqueCardNumber());
        card.setHolderName(user.getName().toUpperCase(Locale.ROOT));
        card.setCvv(generateDigits(3));
        LocalDate expiry = LocalDate.now().plusYears(3);
        card.setExpiryMonth(expiry.getMonthValue());
        card.setExpiryYear(expiry.getYear());
        card.setCreditLimit(50000.0);
        card.setUsedLimit(0.0);
        card.setAvailableBalance(50000.0);
        creditCardRepository.save(card);
        notificationService.create(user.getId(), "welcome", "Welcome to Vibe Bank", "Your account has been created successfully.");
        auditLogService.log(user.getId(), "register", "success", "Registered new user " + user.getUsername());

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        Map<String, Object> response = ApiResponse.successMessage("Registration successful");
        response.put("user_id", user.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("account_number", account.getAccountNumber());
        response.put("access_token", accessToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.getIdentifier()) || isBlank(request.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, "Missing username/email or password");
        }

        String identifier = request.getIdentifier().trim().toLowerCase(Locale.ROOT);
        User user = identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier).orElse(null)
                : userRepository.findByUsernameIgnoreCase(identifier).orElse(null);
        if (user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            return error(HttpStatus.FORBIDDEN, "Account is temporarily locked. Try again later");
        }
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            if (user != null) {
                int failedAttempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(failedAttempts);
                if (failedAttempts >= 5) {
                    user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                    notificationService.create(user.getId(), "security", "Account temporarily locked", "Too many failed login attempts. Try again in 15 minutes.");
                }
                user.touch();
                userRepository.save(user);
                auditLogService.log(user.getId(), "login", "failed", "Failed login attempt");
            } else {
                auditLogService.log(null, "login", "failed", "Failed login attempt for " + identifier);
            }
            return error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!user.isActive()) {
            return error(HttpStatus.FORBIDDEN, "Account is inactive");
        }

        String resolvedRole = resolvePersistedRole(user);
        if (!resolvedRole.equalsIgnoreCase(user.getRole())) {
            user.setRole(resolvedRole);
        }
        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.touch();
        userRepository.save(user);
        auditLogService.log(user.getId(), "login", "success", "Successful login");

        String accessToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        Map<String, Object> response = ApiResponse.successMessage("Login successful");
        response.put("user_id", user.getId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("access_token", accessToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout() {
        return ResponseEntity.ok(ApiResponse.successMessage("Logged out successfully"));
    }

    private String generateUniqueAccountNumber() {
        String value;
        do {
            value = "4082" + generateDigits(12);
        } while (accountRepository.existsByAccountNumber(value));
        return value;
    }

    private String generateUniqueCardNumber() {
        String value;
        do {
            value = "4" + generateDigits(15);
        } while (creditCardRepository.existsByCardNumber(value));
        return value;
    }

    private String generateDigits(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String resolveBootstrapRole(String email) {
        return email != null && email.equalsIgnoreCase(adminEmail) ? "admin" : "user";
    }

    private String resolvePersistedRole(User user) {
        if (user == null) {
            return "user";
        }
        String currentRole = user.getRole();
        if (currentRole != null && !currentRole.isBlank() && !"user".equalsIgnoreCase(currentRole)) {
            return currentRole;
        }
        return resolveBootstrapRole(user.getEmail());
    }
}
