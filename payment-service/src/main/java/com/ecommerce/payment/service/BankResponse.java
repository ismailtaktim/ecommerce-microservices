package com.ecommerce.payment.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankResponse {
    private boolean success;
    private String transactionId;
    private String message;
    private String errorCode;
    @Builder.Default
    private boolean retryLater = false;
}