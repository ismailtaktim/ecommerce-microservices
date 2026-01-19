package com.ecommerce.payment.exception;

public class PaymentDeclinedException extends RuntimeException {
    private final String declineCode;

    public PaymentDeclinedException(String message, String declineCode) {
        super(message);
        this.declineCode = declineCode;
    }

    public String getDeclineCode() {
        return declineCode;
    }
}