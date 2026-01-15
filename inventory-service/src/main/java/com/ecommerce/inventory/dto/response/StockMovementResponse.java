package com.ecommerce.inventory.dto.response;

import com.ecommerce.inventory.entity.MovementType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StockMovementResponse {

    private UUID id;
    private UUID productId;
    private MovementType movementType;
    private Integer quantity;
    private UUID referenceId;
    private String referenceType;
    private String notes;
    private LocalDateTime createdAt;
}