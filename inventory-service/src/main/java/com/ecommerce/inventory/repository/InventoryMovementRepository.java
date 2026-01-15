package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.InventoryMovement;
import com.ecommerce.inventory.entity.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<InventoryMovement> findByMovementType(MovementType movementType);

    List<InventoryMovement> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<InventoryMovement> findByReferenceId(UUID referenceId);
}