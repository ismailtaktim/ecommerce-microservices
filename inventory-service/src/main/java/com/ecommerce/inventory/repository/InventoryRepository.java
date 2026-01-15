package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    Optional<Inventory> findBySku(String sku);

    boolean existsByProductId(UUID productId);

    List<Inventory> findByIsActiveTrue();

    @Query("SELECT i FROM Inventory i WHERE i.isActive = true AND (i.totalQuantity - i.reservedQuantity) <= i.minStockLevel")
    List<Inventory> findLowStockItems();

    @Query("SELECT i FROM Inventory i WHERE i.isActive = true AND (i.totalQuantity - i.reservedQuantity) = 0")
    List<Inventory> findOutOfStockItems();
}