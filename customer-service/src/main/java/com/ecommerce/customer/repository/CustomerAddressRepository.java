package com.ecommerce.customer.repository;

import com.ecommerce.customer.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerIdAndIsActiveTrue(UUID customerId);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<CustomerAddress> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    long countByCustomerIdAndIsActiveTrue(UUID customerId);

    @Modifying
    @Query("UPDATE CustomerAddress a SET a.isDefault = false WHERE a.customer.id = :customerId AND a.isDefault = true")
    void clearDefaultAddress(@Param("customerId") UUID customerId);
}