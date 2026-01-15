package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShippingAddressRequest {

    @NotBlank(message = "Alıcı adı boş olamaz")
    private String recipientName;

    @NotBlank(message = "Telefon boş olamaz")
    private String phone;

    @NotBlank(message = "Adres boş olamaz")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "İlçe boş olamaz")
    private String district;

    @NotBlank(message = "Şehir boş olamaz")
    private String city;

    private String postalCode;

    private String country = "Türkiye";
}