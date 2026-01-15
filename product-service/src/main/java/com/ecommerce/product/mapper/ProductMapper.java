package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.response.ProductImageResponse;
import com.ecommerce.product.dto.response.ProductListResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryMapper categoryMapper;

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .status(product.getStatus())
                .price(product.getPrice())
                .listPrice(product.getListPrice())
                .currency(product.getCurrency())
                .taxRate(product.getTaxRate())
                .category(categoryMapper.toResponse(product.getCategory()))
                .weightGrams(product.getWeightGrams())
                .widthCm(product.getWidthCm())
                .heightCm(product.getHeightCm())
                .depthCm(product.getDepthCm())
                .mainImageUrl(product.getMainImageUrl())
                .images(product.getImages().stream()
                        .map(this::toImageResponse)
                        .collect(Collectors.toList()))
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductListResponse toListResponse(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .listPrice(product.getListPrice())
                .currency(product.getCurrency())
                .mainImageUrl(product.getMainImageUrl())
                .status(product.getStatus())
                .categoryName(product.getCategory().getName())
                .build();
    }

    public ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .sortOrder(image.getSortOrder())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    private ProductListResponse mapToProductListResponse(Product product) {
        return ProductListResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .listPrice(product.getListPrice())
                .currency(product.getCurrency())
                .mainImageUrl(product.getMainImageUrl())
                .status(product.getStatus())
                .categoryName(product.getCategory().getName())
                .build();
    }
}