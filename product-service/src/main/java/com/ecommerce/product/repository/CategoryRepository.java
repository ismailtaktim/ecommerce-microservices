package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullAndIsActiveTrueOrderBySortOrder();

    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrder(UUID parentId);

    List<Category> findByIsActiveTrueOrderByLevelAscSortOrderAsc();

    long countByParentId(UUID parentId);
}