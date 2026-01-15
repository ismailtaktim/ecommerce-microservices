package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelOrderRequest {

    @NotBlank(message = "İptal nedeni boş olamaz")
    private String reason;

    private String cancelledBy;
}