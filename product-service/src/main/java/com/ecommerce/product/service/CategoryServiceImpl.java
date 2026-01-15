package com.ecommerce.product.service;

import com.ecommerce.product.dto.request.CategoryRequest;
import com.ecommerce.product.dto.response.CategoryResponse;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.exception.BadRequestException;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.mapper.CategoryMapper;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    public CategoryResponse create(CategoryRequest request) {
        // 1. Slug benzersiz mi?
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Bu slug zaten kullanılıyor");
        }

        // 2. Parent varsa, geçerli mi?
        Category parent = null;
        int level = 0;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Üst kategori bulunamadı"));
            level = parent.getLevel() + 1;

            // 3. Maksimum 3 seviye kontrolü
            if (level > 2) {
                throw new BadRequestException("Kategori derinliği en fazla 3 seviye olabilir");
            }
        }

        // 4. Kaydet ve dön
        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .parent(parent)
                .imageUrl(request.getImageUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .level(level)
                .isActive(true)
                .build();

        category = categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }

    public CategoryResponse update(UUID id, CategoryRequest request) {
        // 1. Kategori var mı?
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı"));

        // 2. Slug değiştiyse, yeni slug benzersiz mi?
        if (!category.getSlug().equals(request.getSlug()) &&
                categoryRepository.existsBySlug(request.getSlug())) {
            throw new BadRequestException("Bu slug zaten kullanılıyor");
        }

        // 3. Parent değiştiyse kontroller
        if (request.getParentId() != null) {
            // Kendisini parent yapamaz
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Kategori kendisinin üst kategorisi olamaz");
            }

            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Üst kategori bulunamadı"));

            // Kendi alt kategorisini parent yapamaz
            if (isChildCategory(category, parent)) {
                throw new BadRequestException("Alt kategori üst kategori olarak atanamaz");
            }

            category.setParent(parent);
            category.setLevel(parent.getLevel() + 1);
        } else {
            category.setParent(null);
            category.setLevel(0);
        }

        // 4. Güncelle
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setSortOrder(request.getSortOrder());

        category = categoryRepository.save(category);
        return categoryMapper.toResponse(category);
    }
    // Yardımcı metod
    private boolean isChildCategory(Category parent, Category potentialChild) {
        if (potentialChild.getParent() == null) return false;
        if (potentialChild.getParent().getId().equals(parent.getId())) return true;
        return isChildCategory(parent, potentialChild.getParent());
    }

    public void delete(UUID id) {
        // 1. Kategori var mı?
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı"));

        // 2. Alt kategorisi var mı?
        long childCount = categoryRepository.countByParentId(id);
        if (childCount > 0) {
            throw new BadRequestException("Alt kategorileri olan kategori silinemez");
        }

        // 3. Bu kategoride ürün var mı?
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new BadRequestException("Ürün içeren kategori silinemez");
        }

        // 4. Sil
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse getById(UUID id) {
        return null;
    }

    @Override
    public CategoryResponse getBySlug(String slug) {
        return null;
    }

    @Override
    public List<CategoryResponse> getRootCategories() {
        return List.of();
    }

    @Override
    public List<CategoryResponse> getSubCategories(UUID parentId) {
        return List.of();
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByLevelAscSortOrderAsc();
        return categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }
}
