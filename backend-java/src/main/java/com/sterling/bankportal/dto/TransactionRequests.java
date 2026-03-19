package com.sterling.bankportal.dto;

public final class TransactionRequests {

    private TransactionRequests() {
    }

    public static class SimpleTransferRequest {
        private String source_account;
        private String receiver_account;
        private Double amount;
        private String description;

        public String getSource_account() {
            return source_account;
        }

        public void setSource_account(String source_account) {
            this.source_account = source_account;
        }

        public String getReceiver_account() {
            return receiver_account;
        }

        public void setReceiver_account(String receiver_account) {
            this.receiver_account = receiver_account;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class AccountTransferRequest {
        private String source_account;
        private String receiver_account;
        private String receiver_ifsc;
        private String receiver_name;
        private Double amount;
        private String description;

        public String getSource_account() {
            return source_account;
        }

        public void setSource_account(String source_account) {
            this.source_account = source_account;
        }

        public String getReceiver_account() {
            return receiver_account;
        }

        public void setReceiver_account(String receiver_account) {
            this.receiver_account = receiver_account;
        }

        public String getReceiver_ifsc() {
            return receiver_ifsc;
        }

        public void setReceiver_ifsc(String receiver_ifsc) {
            this.receiver_ifsc = receiver_ifsc;
        }

        public String getReceiver_name() {
            return receiver_name;
        }

        public void setReceiver_name(String receiver_name) {
            this.receiver_name = receiver_name;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class UpiTransferRequest {
        private String source_account;
        private String receiver_upi;
        private String receiver_name;
        private Double amount;
        private String description;

        public String getSource_account() {
            return source_account;
        }

        public void setSource_account(String source_account) {
            this.source_account = source_account;
        }

        public String getReceiver_upi() {
            return receiver_upi;
        }

        public void setReceiver_upi(String receiver_upi) {
            this.receiver_upi = receiver_upi;
        }

        public String getReceiver_name() {
            return receiver_name;
        }

        public void setReceiver_name(String receiver_name) {
            this.receiver_name = receiver_name;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
