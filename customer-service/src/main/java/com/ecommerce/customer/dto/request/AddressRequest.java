package com.ecommerce.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Adres başlığı boş olamaz")
    private String title;

    @NotBlank(message = "Alıcı adı boş olamaz")
    private String recipientName;

    @NotBlank(message = "Alıcı telefonu boş olamaz")
    private String recipientPhone;

    @NotBlank(message = "Adres boş olamaz")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "İlçe boş olamaz")
    private String district;

    @NotBlank(message = "Şehir boş olamaz")
    private String city;

    private String postalCode;

    private String country = "Türkiye";

    private Boolean isDefault = false;
}