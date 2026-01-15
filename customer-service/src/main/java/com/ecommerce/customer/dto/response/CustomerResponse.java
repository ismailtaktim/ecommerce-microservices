package com.ecommerce.customer.dto.response;

import com.ecommerce.customer.entity.CustomerRole;
import com.ecommerce.customer.entity.CustomerStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CustomerResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private CustomerStatus status;
    private CustomerRole role;
    private Boolean emailVerified;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}