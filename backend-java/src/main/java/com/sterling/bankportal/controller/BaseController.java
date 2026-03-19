package com.sterling.bankportal.controller;

import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.AccountRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.security.AuthUser;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

    protected ResponseEntity<Object> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(com.sterling.bankportal.util.ApiResponse.error(message));
    }

    protected String userId(Principal principal) {
        return ((AuthUser) ((org.springframework.security.core.Authentication) principal).getPrincipal()).getUserId();
    }

    protected User requireUser(Principal principal, UserRepository userRepository) {
        return userRepository.findById(userId(principal)).orElse(null);
    }

    protected Account requireAccount(String userId, AccountRepository accountRepository) {
        return accountRepository.findFirstByUserId(userId).orElse(null);
    }
}
