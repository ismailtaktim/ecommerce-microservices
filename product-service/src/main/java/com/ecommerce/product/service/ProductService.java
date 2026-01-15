package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.ProductImageRequest;
import com.ecommerce.product.dto.request.ProductRequest;
import com.ecommerce.product.dto.request.ProductSearchRequest;
import com.ecommerce.product.dto.response.ProductListResponse;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {
    ProductResponse create(ProductRequest request);
    ProductResponse update(UUID id, ProductRequest request);
    void delete(UUID id);
    ProductResponse getById(UUID id);
    ProductResponse getBySku(String sku);
    ProductResponse getBySlug(String slug);
    Page<ProductListResponse> getAll(Pageable pageable);
    Page<ProductListResponse> getByCategory(UUID categoryId, Pageable pageable);
    Page<ProductListResponse> search(ProductSearchRequest request);
    ProductResponse updateStatus(UUID id, ProductStatus status);
    ProductResponse addImage(UUID productId, ProductImageRequest request);
    void removeImage(UUID productId, UUID imageId);
}
