package com.ecommerce.product.dto.request;

import com.ecommerce.product.entity.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductSearchRequest {

    private String keyword;

    private UUID categoryId;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private ProductStatus status = ProductStatus.ACTIVE;

    private Integer page = 0;

    private Integer size = 20;

    private String sortBy = "createdAt";

    private String sortDirection = "DESC";
}