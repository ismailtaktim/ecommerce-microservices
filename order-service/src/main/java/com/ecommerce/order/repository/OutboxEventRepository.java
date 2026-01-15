package com.ecommerce.order.repository;

import com.ecommerce.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.published = true, e.publishedAt = :now WHERE e.id = :id")
    void markAsPublished(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.published = true AND e.publishedAt < :before")
    void deletePublishedEventsBefore(@Param("before") LocalDateTime before);
}