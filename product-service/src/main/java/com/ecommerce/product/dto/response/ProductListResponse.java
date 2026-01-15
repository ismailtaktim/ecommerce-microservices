package com.ecommerce.product.dto.response;

import com.ecommerce.product.entity.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductListResponse {

    private UUID id;
    private String sku;
    private String name;
    private String slug;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal listPrice;
    private String currency;
    private String mainImageUrl;
    private ProductStatus status;
    private String categoryName;
}