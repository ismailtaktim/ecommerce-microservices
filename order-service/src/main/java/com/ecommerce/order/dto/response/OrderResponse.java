package com.ecommerce.order.dto.response;

import com.ecommerce.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {

    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private String customerEmail;
    private String customerPhone;
    private OrderStatus status;

    // Teslimat Adresi
    private String shippingRecipientName;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingDistrict;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;

    // Finansal
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;

    // İptal Bilgileri
    private String cancellationReason;
    private String failureReason;

    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}