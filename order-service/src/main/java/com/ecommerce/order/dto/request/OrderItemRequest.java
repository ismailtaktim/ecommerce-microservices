package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OrderItemRequest {

    @NotNull(message = "Ürün ID boş olamaz")
    private UUID productId;

    @NotBlank(message = "Ürün adı boş olamaz")
    private String productName;

    @NotBlank(message = "SKU boş olamaz")
    private String productSku;

    @NotNull(message = "Miktar boş olamaz")
    @Min(value = 1, message = "Miktar en az 1 olmalıdır")
    private Integer quantity;

    @NotNull(message = "Birim fiyat boş olamaz")
    private BigDecimal unitPrice;
}