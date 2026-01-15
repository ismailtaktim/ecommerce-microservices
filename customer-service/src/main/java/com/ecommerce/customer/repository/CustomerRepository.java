package com.ecommerce.customer.repository;

import com.ecommerce.customer.entity.Customer;
import com.ecommerce.customer.entity.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmailVerificationToken(String token);

    Optional<Customer> findByPasswordResetToken(String token);

    long countByStatus(CustomerStatus status);
}