package com.ecommerce.payment.exception;

public class BankConnectionException extends RuntimeException {
    public BankConnectionException(String message) {
        super(message);
    }

    public BankConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}