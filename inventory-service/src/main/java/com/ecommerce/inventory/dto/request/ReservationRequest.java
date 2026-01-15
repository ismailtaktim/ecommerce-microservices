package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReservationRequest {

    @NotNull(message = "Order ID boş olamaz")
    private UUID orderId;

    @NotEmpty(message = "En az bir ürün olmalıdır")
    private List<ReservationItemRequest> items;

    private Integer expirationMinutes = 15;
}