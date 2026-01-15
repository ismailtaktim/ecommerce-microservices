package com.ecommerce.order.dto.response;

import com.ecommerce.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderListResponse {

    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private Integer itemCount;
    private LocalDateTime createdAt;
}