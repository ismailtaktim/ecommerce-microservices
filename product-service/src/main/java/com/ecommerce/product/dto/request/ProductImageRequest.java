package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductImageRequest {

    @NotBlank(message = "Görsel URL boş olamaz")
    private String imageUrl;

    private String altText;

    private Integer sortOrder = 0;

    private Boolean isPrimary = false;
}