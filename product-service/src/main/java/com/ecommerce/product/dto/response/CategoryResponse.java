package com.ecommerce.product.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private UUID parentId;
    private String parentName;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private Integer level;
    private Long childCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}