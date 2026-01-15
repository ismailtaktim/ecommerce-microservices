package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.CategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(UUID id, CategoryRequest request);
    void delete(UUID id);
    CategoryResponse getById(UUID id);
    CategoryResponse getBySlug(String slug);
    List<CategoryResponse> getRootCategories();
    List<CategoryResponse> getSubCategories(UUID parentId);
    List<CategoryResponse> getAllCategories();
}
