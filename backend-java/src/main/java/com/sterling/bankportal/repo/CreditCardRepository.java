package com.sterling.bankportal.repo;

import com.sterling.bankportal.model.CreditCard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardRepository extends JpaRepository<CreditCard, String> {
    Optional<CreditCard> findFirstByUserId(String userId);
    boolean existsByCardNumber(String cardNumber);
}
