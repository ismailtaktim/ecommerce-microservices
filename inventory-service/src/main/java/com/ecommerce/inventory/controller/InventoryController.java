package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.request.*;
import com.ecommerce.inventory.dto.response.*;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== INVENTORY CRUD ====================

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody CreateInventoryRequest request) {
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stok kaydı oluşturuldu", response));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProductId(@PathVariable UUID productId) {
        InventoryResponse response = inventoryService.getByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getBySku(@PathVariable String sku) {
        InventoryResponse response = inventoryService.getBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventories() {
        List<InventoryResponse> response = inventoryService.getAllInventories();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockItems() {
        List<InventoryResponse> response = inventoryService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== STOCK OPERATIONS ====================

    @PostMapping("/product/{productId}/add")
    public ResponseEntity<ApiResponse<InventoryResponse>> addStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockUpdateRequest request) {
        InventoryResponse response = inventoryService.addStock(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Stok eklendi", response));
    }

    @PostMapping("/product/{productId}/remove")
    public ResponseEntity<ApiResponse<InventoryResponse>> removeStock(
            @PathVariable UUID productId,
            @Valid @RequestBody StockUpdateRequest request) {
        InventoryResponse response = inventoryService.removeStock(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Stok çıkışı yapıldı", response));
    }

    @PutMapping("/product/{productId}/adjust")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable UUID productId,
            @RequestParam Integer newQuantity,
            @RequestParam(required = false) String notes) {
        InventoryResponse response = inventoryService.adjustStock(productId, newQuantity, notes);
        return ResponseEntity.ok(ApiResponse.success("Stok düzeltildi", response));
    }

    // ==================== RESERVATIONS ====================

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse response = inventoryService.createReservation(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rezervasyon oluşturuldu", response));
    }

    @PostMapping("/reservations/{orderId}/confirm")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirmReservation(@PathVariable UUID orderId) {
        ReservationResponse response = inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok(ApiResponse.success("Rezervasyon onaylandı", response));
    }

    @PostMapping("/reservations/{orderId}/release")
    public ResponseEntity<ApiResponse<ReservationResponse>> releaseReservation(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason) {
        ReservationResponse response = inventoryService.releaseReservation(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Rezervasyon serbest bırakıldı", response));
    }

    @GetMapping("/reservations/{orderId}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(@PathVariable UUID orderId) {
        ReservationResponse response = inventoryService.getReservationByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== STOCK MOVEMENTS ====================

    @GetMapping("/product/{productId}/movements")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getMovements(@PathVariable UUID productId) {
        List<StockMovementResponse> response = inventoryService.getMovementsByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}