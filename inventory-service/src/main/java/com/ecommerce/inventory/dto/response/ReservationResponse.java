package com.ecommerce.inventory.dto.response;

import com.ecommerce.inventory.entity.ReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReservationResponse {

    private UUID id;
    private UUID orderId;
    private ReservationStatus status;
    private List<ReservationItemResponse> items;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime releasedAt;
    private String releaseReason;
    private LocalDateTime createdAt;
}