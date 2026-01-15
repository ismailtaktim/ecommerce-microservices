package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    @NotNull(message = "Müşteri ID boş olamaz")
    private UUID customerId;

    @NotNull(message = "Müşteri email boş olamaz")
    private String customerEmail;

    private String customerPhone;

    @NotNull(message = "Teslimat adresi boş olamaz")
    private ShippingAddressRequest shippingAddress;

    @NotEmpty(message = "En az bir ürün olmalıdır")
    private List<OrderItemRequest> items;
}