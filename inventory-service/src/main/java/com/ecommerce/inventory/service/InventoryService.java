package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.request.*;
import com.ecommerce.inventory.dto.response.*;
import com.ecommerce.inventory.event.InventoryReserveRequestEvent;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    // Inventory CRUD
    InventoryResponse createInventory(CreateInventoryRequest request);
    InventoryResponse getByProductId(UUID productId);
    InventoryResponse getBySku(String sku);
    List<InventoryResponse> getAllInventories();
    List<InventoryResponse> getLowStockItems();

    // Stock Operations
    InventoryResponse addStock(UUID productId, StockUpdateRequest request);
    InventoryResponse removeStock(UUID productId, StockUpdateRequest request);
    InventoryResponse adjustStock(UUID productId, Integer newQuantity, String notes);

    // Reservation Operations
    ReservationResponse createReservation(ReservationRequest request);
    ReservationResponse confirmReservation(UUID orderId);
    ReservationResponse releaseReservation(UUID orderId, String reason);
    ReservationResponse getReservationByOrderId(UUID orderId);

    // Stock Movements
    List<StockMovementResponse> getMovementsByProductId(UUID productId);

    // Event-driven metodlar
    void reserveInventoryForOrder(InventoryReserveRequestEvent event);
    void releaseInventoryForOrder(UUID orderId, String reason);
}