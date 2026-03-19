package com.sterling.bankportal.util;

import java.util.regex.Pattern;

public final class Validators {

    private static final Pattern EMAIL = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$");
    private static final Pattern UPI = Pattern.compile("^[a-zA-Z0-9._]{3,}@[a-zA-Z]{3,}$");
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private Validators() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        if (username.trim().length() < 3 || username.trim().length() > 100) {
            return "Username must be between 3 and 100 characters";
        }
        return null;
    }

    public static String validateAccountHolderName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Account holder name is required";
        }
        if (name.trim().length() < 3 || name.trim().length() > 100) {
            return "Account holder name must be between 3 and 100 characters";
        }
        return null;
    }

    public static String validateEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "Invalid email format";
        }
        String[] parts = email.split("@", 2);
        if (parts.length != 2 || parts[1].isBlank()) {
            return "Invalid email domain";
        }

        String domain = parts[1].toLowerCase();
        String[] labels = domain.split("\\.");
        if (labels.length < 2) {
            return "Email domain must include a valid top-level domain";
        }
        for (String label : labels) {
            if (label.isBlank() || label.startsWith("-") || label.endsWith("-")) {
                return "Invalid email domain";
            }
        }
        if (labels[labels.length - 1].length() < 2) {
            return "Email domain must include a valid top-level domain";
        }
        return null;
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    public static String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters";
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            return "Password must contain at least one digit";
        }
        if (password.chars().allMatch(Character::isLetterOrDigit)) {
            return "Password must contain at least one special character";
        }
        return null;
    }

    public static boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null && accountNumber.matches("\\d{8,16}");
    }

    public static boolean isValidUpi(String upi) {
        return upi != null && UPI.matcher(upi).matches();
    }

    public static boolean isValidIfsc(String ifsc) {
        return ifsc != null && IFSC.matcher(ifsc).matches();
    }

    public static boolean isValidTransactionPin(String pin) {
        return pin != null && pin.matches("\\d{4}");
    }

    public static boolean isPositiveAmount(Double amount) {
        return amount != null && amount > 0;
    }
}
