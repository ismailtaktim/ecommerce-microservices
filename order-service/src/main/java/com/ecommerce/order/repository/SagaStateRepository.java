package com.ecommerce.order.repository;

import com.ecommerce.order.entity.SagaState;
import com.ecommerce.order.entity.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    Optional<SagaState> findByOrderId(UUID orderId);

    List<SagaState> findByStatus(SagaStatus status);

    List<SagaState> findByStatusIn(List<SagaStatus> statuses);
}