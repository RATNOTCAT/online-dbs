package com.sterling.bankportal.util;

import com.sterling.bankportal.model.Account;
import com.sterling.bankportal.model.CreditCard;
import com.sterling.bankportal.model.SavingsGoal;
import com.sterling.bankportal.model.Transaction;
import com.sterling.bankportal.model.User;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static Map<String, Object> user(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("phone", user.getPhone());
        data.put("address", user.getAddress());
        data.put("date_of_birth", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
        data.put("aadhar_number", user.getAadharNumber());
        data.put("pan_number", user.getPanNumber());
        data.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        data.put("is_active", user.isActive());
        return data;
    }

    public static Map<String, Object> account(Account account) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", account.getId());
        data.put("account_number", account.getAccountNumber());
        data.put("ifsc_code", account.getIfscCode());
        data.put("account_type", account.getAccountType());
        data.put("balance", account.getBalance());
        data.put("overdraft_limit", account.getOverdraftLimit());
        data.put("is_active", account.isActive());
        data.put("created_at", account.getCreatedAt() != null ? account.getCreatedAt().toString() : null);
        return data;
    }

    public static Map<String, Object> creditCard(CreditCard card) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", card.getId());
        data.put("holder_name", card.getHolderName());
        data.put("card_number", card.getMaskedNumber());
        data.put("expiry", card.getExpiryString());
        data.put("cvv", card.getCvv());
        data.put("credit_limit", card.getCreditLimit());
        data.put("used_limit", card.getUsedLimit());
        data.put("available_balance", card.getAvailableBalance());
        data.put("is_active", card.isActive());
        data.put("created_at", card.getCreatedAt() != null ? card.getCreatedAt().toString() : null);
        return data;
    }

    public static Map<String, Object> transaction(Transaction transaction) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", transaction.getId());
        data.put("sender_id", transaction.getSenderId());
        data.put("receiver_id", transaction.getReceiverId());
        data.put("entry_type", transaction.getEntryType());
        data.put("type", transaction.getType());
        data.put("method", transaction.getMethod());
        data.put("amount", transaction.getAmount());
        data.put("description", transaction.getDescription());
        data.put("sender_name", transaction.getSenderName());
        data.put("sender_account_no", transaction.getSenderAccountNo());
        data.put("receiver_name", transaction.getReceiverName());
        data.put("receiver_account_no", transaction.getReceiverAccountNo());
        data.put("receiver_ifsc", transaction.getReceiverIfsc());
        data.put("receiver_upi", transaction.getReceiverUpi());
        data.put("status", transaction.getStatus());
        data.put("reference_number", transaction.getReferenceNumber());
        data.put("created_at", transaction.getCreatedAt() != null ? transaction.getCreatedAt().toString() : null);
        data.put("completed_at", transaction.getCompletedAt() != null ? transaction.getCompletedAt().toString() : null);
        return data;
    }

    public static Map<String, Object> goal(SavingsGoal goal) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", goal.getId());
        data.put("title", goal.getTitle());
        data.put("description", goal.getDescription());
        data.put("category", goal.getCategory());
        data.put("target_amount", goal.getTargetAmount());
        data.put("saved_amount", goal.getSavedAmount());
        data.put("remaining_amount", Math.max(0, goal.getTargetAmount() - goal.getSavedAmount()));
        data.put("progress_percent", goal.getTargetAmount() <= 0 ? 0 : Math.min(100, (goal.getSavedAmount() / goal.getTargetAmount()) * 100));
        data.put("target_date", goal.getTargetDate() != null ? goal.getTargetDate().toString() : null);
        data.put("status", goal.getStatus());
        data.put("created_at", goal.getCreatedAt() != null ? goal.getCreatedAt().toString() : null);
        data.put("updated_at", goal.getUpdatedAt() != null ? goal.getUpdatedAt().toString() : null);
        return data;
    }
}
