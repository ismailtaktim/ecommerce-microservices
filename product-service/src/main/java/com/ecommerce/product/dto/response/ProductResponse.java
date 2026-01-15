package com.ecommerce.product.dto.response;

import com.ecommerce.product.entity.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String sku;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private ProductStatus status;
    private BigDecimal price;
    private BigDecimal listPrice;
    private String currency;
    private BigDecimal taxRate;
    private CategoryResponse category;
    private Integer weightGrams;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal depthCm;
    private String mainImageUrl;
    private List<ProductImageResponse> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}