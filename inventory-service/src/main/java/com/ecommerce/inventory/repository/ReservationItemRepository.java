package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.entity.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationItemRepository extends JpaRepository<ReservationItem, UUID> {

    List<ReservationItem> findByReservationId(UUID reservationId);

    List<ReservationItem> findByProductId(UUID productId);
}