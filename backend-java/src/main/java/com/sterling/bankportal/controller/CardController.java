package com.sterling.bankportal.controller;

import com.sterling.bankportal.model.CreditCard;
import com.sterling.bankportal.model.User;
import com.sterling.bankportal.repo.CreditCardRepository;
import com.sterling.bankportal.repo.UserRepository;
import com.sterling.bankportal.util.ApiResponse;
import com.sterling.bankportal.util.ResponseMapper;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CardController extends BaseController {

    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;

    public CardController(UserRepository userRepository, CreditCardRepository creditCardRepository) {
        this.userRepository = userRepository;
        this.creditCardRepository = creditCardRepository;
    }

    @GetMapping("/credit-card")
    public ResponseEntity<Object> getCreditCard(Principal principal) {
        User user = requireUser(principal, userRepository);
        if (user == null) {
            return error(HttpStatus.NOT_FOUND, "User not found");
        }

        CreditCard card = creditCardRepository.findFirstByUserId(user.getId()).orElse(null);
        if (card == null) {
            return error(HttpStatus.NOT_FOUND, "Credit card not found");
        }

        Map<String, Object> response = ApiResponse.success();
        response.put("card", ResponseMapper.creditCard(card));
        return ResponseEntity.ok(response);
    }
}
