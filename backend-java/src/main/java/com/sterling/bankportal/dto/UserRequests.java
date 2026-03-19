package com.sterling.bankportal.dto;

public final class UserRequests {

    private UserRequests() {
    }

    public static class UpdateProfileRequest {
        private String name;
        private String phone;
        private String address;
        private String date_of_birth;
        private String aadhar_number;
        private String pan_number;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getDate_of_birth() {
            return date_of_birth;
        }

        public void setDate_of_birth(String date_of_birth) {
            this.date_of_birth = date_of_birth;
        }

        public String getAadhar_number() {
            return aadhar_number;
        }

        public void setAadhar_number(String aadhar_number) {
            this.aadhar_number = aadhar_number;
        }

        public String getPan_number() {
            return pan_number;
        }

        public void setPan_number(String pan_number) {
            this.pan_number = pan_number;
        }
    }

    public static class ChangePasswordRequest {
        private String old_password;
        private String new_password;

        public String getOld_password() {
            return old_password;
        }

        public void setOld_password(String old_password) {
            this.old_password = old_password;
        }

        public String getNew_password() {
            return new_password;
        }

        public void setNew_password(String new_password) {
            this.new_password = new_password;
        }
    }

    public static class SetTransactionPinRequest {
        private String pin;
        private String password;

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
