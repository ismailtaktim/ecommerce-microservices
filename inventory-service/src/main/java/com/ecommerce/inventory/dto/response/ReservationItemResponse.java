package com.ecommerce.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ReservationItemResponse {

    private UUID id;
    private UUID productId;
    private Integer quantity;
}