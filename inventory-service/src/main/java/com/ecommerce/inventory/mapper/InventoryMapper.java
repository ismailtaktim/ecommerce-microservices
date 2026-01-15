package com.ecommerce.inventory.mapper;

import com.ecommerce.inventory.dto.response.InventoryResponse;
import com.ecommerce.inventory.dto.response.ReservationItemResponse;
import com.ecommerce.inventory.dto.response.ReservationResponse;
import com.ecommerce.inventory.dto.response.StockMovementResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.InventoryMovement;
import com.ecommerce.inventory.entity.Reservation;
import com.ecommerce.inventory.entity.ReservationItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .sku(inventory.getSku())
                .totalQuantity(inventory.getTotalQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .minStockLevel(inventory.getMinStockLevel())
                .isLowStock(inventory.isLowStock())
                .isActive(inventory.getIsActive())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .orderId(reservation.getOrderId())
                .status(reservation.getStatus())
                .items(reservation.getItems().stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .expiresAt(reservation.getExpiresAt())
                .confirmedAt(reservation.getConfirmedAt())
                .releasedAt(reservation.getReleasedAt())
                .releaseReason(reservation.getReleaseReason())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    public ReservationItemResponse toResponse(ReservationItem item) {
        return ReservationItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .build();
    }

    public StockMovementResponse toResponse(InventoryMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getProductId())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .referenceId(movement.getReferenceId())
                .referenceType(movement.getReferenceType())
                .notes(movement.getNotes())
                .createdAt(movement.getCreatedAt())
                .build();
    }
}