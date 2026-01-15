package com.ecommerce.customer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AddressResponse {

    private UUID id;
    private String title;
    private String recipientName;
    private String recipientPhone;
    private String addressLine1;
    private String addressLine2;
    private String district;
    private String city;
    private String postalCode;
    private String country;
    private Boolean isDefault;
}