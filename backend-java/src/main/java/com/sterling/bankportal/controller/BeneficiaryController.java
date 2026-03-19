package com.sterling.bankportal.controller;

import com.sterling.bankportal.dto.BeneficiaryRequests.CreateBeneficiaryRequest;
import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.Beneficiary;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.BeneficiaryRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.service.AuditLogService;
import com.sterling.bankportal.service.NotificationService;
import com.sterling.bankportal.util.ApiResponse;
import com.sterling.bankportal.util.Validators;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController extends BaseController {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public BeneficiaryController(
            BeneficiaryRepository beneficiaryRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            NotificationService notificationService,
            AuditLogService auditLogService) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Object> listBeneficiaries(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Map<String, Object>> beneficiaries = beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toPayload)
                .toList();

        Map<String, Object> response = ApiResponse.success();
        response.put("beneficiaries", beneficiaries);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> createBeneficiary(Principal principal, @RequestBody CreateBeneficiaryRequest request) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }
        if (request == null || request.getType() == null || request.getName() == null) {
            return error(HttpStatus.BAD_REQUEST, "Missing required fields");
        }

        String type = request.getType().trim().toLowerCase(Locale.ROOT);
        if (!type.equals("account") && !type.equals("upi")) {
            return error(HttpStatus.BAD_REQUEST, "Beneficiary type must be account or upi");
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setUserId(user.getId());
        beneficiary.setType(type);
        beneficiary.setName(request.getName().trim());
        beneficiary.setNickname(request.getNickname() != null ? request.getNickname().trim() : null);

        if (type.equals("account")) {
            if (request.getAccount_number() == null) {
                return error(HttpStatus.BAD_REQUEST, "Account number is required");
            }
            String accountNumber = request.getAccount_number().trim();
            if (!Validators.isValidAccountNumber(accountNumber)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid account number");
            }
            String ifsc = request.getIfsc_code() != null && !request.getIfsc_code().trim().isEmpty()
                    ? request.getIfsc_code().trim().toUpperCase(Locale.ROOT)
                    : null;
            if (ifsc == null) {
                Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
                ifsc = account != null ? account.getIfscCode() : null;
            }
            if (ifsc != null && !Validators.isValidIfsc(ifsc)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid IFSC code");
            }
            if (beneficiaryRepository.existsByUserIdAndTypeAndAccountNumber(user.getId(), type, accountNumber)) {
                return error(HttpStatus.BAD_REQUEST, "Beneficiary already exists");
            }
            beneficiary.setAccountNumber(accountNumber);
            beneficiary.setIfscCode(ifsc);
        } else {
            if (request.getUpi_id() == null) {
                return error(HttpStatus.BAD_REQUEST, "UPI ID is required");
            }
            String upiId = request.getUpi_id().trim().toLowerCase(Locale.ROOT);
            if (!Validators.isValidUpi(upiId)) {
                return error(HttpStatus.BAD_REQUEST, "Invalid UPI ID");
            }
            if (beneficiaryRepository.existsByUserIdAndTypeAndUpiId(user.getId(), type, upiId)) {
                return error(HttpStatus.BAD_REQUEST, "Beneficiary already exists");
            }
            beneficiary.setUpiId(upiId);
        }

        beneficiaryRepository.save(beneficiary);
        notificationService.create(user.getId(), "beneficiary", "Beneficiary saved", "Saved " + beneficiary.getName() + " for faster transfers.");
        auditLogService.log(user.getId(), "beneficiary_create", "success", "Saved beneficiary " + beneficiary.getName());

        Map<String, Object> response = ApiResponse.successMessage("Beneficiary saved successfully");
        response.put("beneficiary", toPayload(beneficiary));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{beneficiaryId}")
    @Transactional
    public ResponseEntity<Object> deleteBeneficiary(Principal principal, @PathVariable String beneficiaryId) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, user.getId()).orElse(null);
        if (beneficiary == null) {
            return error(HttpStatus.NOT_FOUND, "Beneficiary not found");
        }

        beneficiaryRepository.delete(beneficiary);
        auditLogService.log(user.getId(), "beneficiary_delete", "success", "Deleted beneficiary " + beneficiary.getName());
        return ResponseEntity.ok(ApiResponse.successMessage("Beneficiary deleted successfully"));
    }

    private Map<String, Object> toPayload(Beneficiary beneficiary) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", beneficiary.getId());
        payload.put("type", beneficiary.getType());
        payload.put("name", beneficiary.getName());
        payload.put("nickname", beneficiary.getNickname());
        payload.put("account_number", beneficiary.getAccountNumber());
        payload.put("ifsc_code", beneficiary.getIfscCode());
        payload.put("upi_id", beneficiary.getUpiId());
        payload.put("created_at", beneficiary.getCreatedAt() != null ? beneficiary.getCreatedAt().toString() : null);
        return payload;
    }
}
