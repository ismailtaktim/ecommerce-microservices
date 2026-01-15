package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CategoryRequest {

    @NotBlank(message = "Kategori adı boş olamaz")
    private String name;

    @NotBlank(message = "Slug boş olamaz")
    private String slug;

    private String description;

    private UUID parentId;

    private String imageUrl;

    private Integer sortOrder = 0;
}