package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(UUID customerId);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    List<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByCustomerId(UUID customerId);

    long countByStatus(OrderStatus status);

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}