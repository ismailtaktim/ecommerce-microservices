package com.ecommerce.order.service;

import com.ecommerce.order.dto.request.CancelOrderRequest;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderListResponse;
import com.ecommerce.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    // Order CRUD
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(UUID orderId);
    OrderResponse getOrderByNumber(String orderNumber);
    List<OrderResponse> getOrdersByCustomerId(UUID customerId);
    Page<OrderListResponse> getAllOrders(Pageable pageable);

    // Order Operations
    OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request);

    // Saga Callbacks (diğer servislerden gelen event'ler için)
    void handleInventoryReserved(UUID orderId, boolean success, String failureReason);
    void handlePaymentCompleted(UUID orderId, boolean success, String failureReason);
}