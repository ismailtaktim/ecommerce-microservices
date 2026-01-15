package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.request.*;
import com.ecommerce.inventory.dto.response.*;
import com.ecommerce.inventory.entity.*;
import com.ecommerce.inventory.event.InventoryReserveRequestEvent;
import com.ecommerce.inventory.event.InventoryReservedEvent;
import com.ecommerce.inventory.exception.*;
import com.ecommerce.inventory.kafka.InventoryKafkaProducer;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryKafkaProducer kafkaProducer;

    // ==================== INVENTORY CRUD ====================

    @Override
    public InventoryResponse createInventory(CreateInventoryRequest request) {
        // 1. Product ID zaten var mı?
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new BadRequestException("Bu ürün için stok kaydı zaten mevcut");
        }

        // 2. Inventory oluştur
        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .productName(request.getProductName())
                .sku(request.getSku())
                .totalQuantity(request.getInitialQuantity())
                .reservedQuantity(0)
                .minStockLevel(request.getMinStockLevel())
                .isActive(true)
                .build();

        inventory = inventoryRepository.save(inventory);

        // 3. İlk stok hareketi kaydet
        if (request.getInitialQuantity() > 0) {
            createMovement(inventory.getProductId(), MovementType.STOCK_IN,
                    request.getInitialQuantity(), null, null, "İlk stok girişi");
        }

        log.info("Stok kaydı oluşturuldu: {} - {}", inventory.getSku(), inventory.getTotalQuantity());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok kaydı bulunamadı"));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getBySku(String sku) {
        Inventory inventory = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Stok kaydı bulunamadı"));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventories() {
        return inventoryRepository.findByIsActiveTrue().stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockItems() {
        return inventoryRepository.findLowStockItems().stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== STOCK OPERATIONS ====================

    @Override
    public InventoryResponse addStock(UUID productId, StockUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok kaydı bulunamadı"));

        inventory.setTotalQuantity(inventory.getTotalQuantity() + request.getQuantity());
        inventory = inventoryRepository.save(inventory);

        createMovement(productId, MovementType.STOCK_IN, request.getQuantity(),
                null, null, request.getNotes());

        log.info("Stok eklendi: {} - +{}", inventory.getSku(), request.getQuantity());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse removeStock(UUID productId, StockUpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok kaydı bulunamadı"));

        // Yeterli stok var mı?
        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientStockException("Yetersiz stok. Mevcut: " + inventory.getAvailableQuantity());
        }

        inventory.setTotalQuantity(inventory.getTotalQuantity() - request.getQuantity());
        inventory = inventoryRepository.save(inventory);

        createMovement(productId, MovementType.STOCK_OUT, request.getQuantity(),
                null, null, request.getNotes());

        log.info("Stok çıkışı: {} - -{}", inventory.getSku(), request.getQuantity());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse adjustStock(UUID productId, Integer newQuantity, String notes) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok kaydı bulunamadı"));

        int difference = newQuantity - inventory.getTotalQuantity();
        inventory.setTotalQuantity(newQuantity);
        inventory = inventoryRepository.save(inventory);

        createMovement(productId, MovementType.ADJUSTMENT, Math.abs(difference),
                null, null, notes != null ? notes : "Stok düzeltmesi: " + difference);

        log.info("Stok düzeltildi: {} - Yeni miktar: {}", inventory.getSku(), newQuantity);
        return inventoryMapper.toResponse(inventory);
    }

    // ==================== RESERVATION OPERATIONS ====================

    @Override
    public ReservationResponse createReservation(ReservationRequest request) {
        // 1. Bu sipariş için zaten rezervasyon var mı?
        if (reservationRepository.existsByOrderId(request.getOrderId())) {
            throw new BadRequestException("Bu sipariş için zaten rezervasyon mevcut");
        }

        // 2. Tüm ürünler için stok kontrolü
        for (ReservationItemRequest item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ürün stok kaydı bulunamadı: " + item.getProductId()));

            if (!inventory.hasAvailableStock(item.getQuantity())) {
                throw new InsufficientStockException(
                        "Yetersiz stok: " + inventory.getProductName() +
                                " - İstenen: " + item.getQuantity() +
                                ", Mevcut: " + inventory.getAvailableQuantity());
            }
        }

        // 3. Rezervasyon oluştur
        Reservation reservation = Reservation.builder()
                .orderId(request.getOrderId())
                .status(ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(request.getExpirationMinutes()))
                .build();

        reservation = reservationRepository.save(reservation);

        // 4. Rezervasyon itemları oluştur ve stokları güncelle
        for (ReservationItemRequest itemRequest : request.getItems()) {
            ReservationItem item = ReservationItem.builder()
                    .reservation(reservation)
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .build();

            reservation.getItems().add(item);

            // Stoku rezerve et
            Inventory inventory = inventoryRepository.findByProductId(itemRequest.getProductId()).get();
            inventory.setReservedQuantity(inventory.getReservedQuantity() + itemRequest.getQuantity());
            inventoryRepository.save(inventory);

            createMovement(itemRequest.getProductId(), MovementType.RESERVATION,
                    itemRequest.getQuantity(), reservation.getId(), "RESERVATION", null);
        }

        reservation = reservationRepository.save(reservation);
        log.info("Rezervasyon oluşturuldu: Order={}", request.getOrderId());
        return inventoryMapper.toResponse(reservation);
    }

    @Override
    public ReservationResponse confirmReservation(UUID orderId) {
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasyon bulunamadı"));

        // 1. Durum kontrolü
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("Sadece bekleyen rezervasyonlar onaylanabilir");
        }

        // 2. Süre kontrolü
        if (reservation.isExpired()) {
            throw new BadRequestException("Rezervasyon süresi dolmuş");
        }

        // 3. Onayla - rezerve edilen stokları düş
        for (ReservationItem item : reservation.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).get();
            inventory.setTotalQuantity(inventory.getTotalQuantity() - item.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);

            createMovement(item.getProductId(), MovementType.SALE,
                    item.getQuantity(), reservation.getId(), "ORDER", null);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

        log.info("Rezervasyon onaylandı: Order={}", orderId);
        return inventoryMapper.toResponse(reservation);
    }

    @Override
    public ReservationResponse releaseReservation(UUID orderId, String reason) {
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasyon bulunamadı"));

        // 1. Durum kontrolü
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("Sadece bekleyen rezervasyonlar iptal edilebilir");
        }

        // 2. Rezervasyonu serbest bırak
        for (ReservationItem item : reservation.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).get();
            inventory.setReservedQuantity(inventory.getReservedQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);

            createMovement(item.getProductId(), MovementType.RESERVATION_CANCEL,
                    item.getQuantity(), reservation.getId(), "RESERVATION", reason);
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(LocalDateTime.now());
        reservation.setReleaseReason(reason);
        reservation = reservationRepository.save(reservation);

        log.info("Rezervasyon serbest bırakıldı: Order={}, Reason={}", orderId, reason);
        return inventoryMapper.toResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationByOrderId(UUID orderId) {
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervasyon bulunamadı"));
        return inventoryMapper.toResponse(reservation);
    }

    // ==================== STOCK MOVEMENTS ====================

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementResponse> getMovementsByProductId(UUID productId) {
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reserveInventoryForOrder(InventoryReserveRequestEvent event) {
        log.info("Sipariş için stok rezervasyonu başlatılıyor: orderId={}", event.getOrderId());

        try {
            // 1. Tüm ürünler için stok kontrolü yap
            for (InventoryReserveRequestEvent.OrderItemEvent item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Ürün stoğu bulunamadı: " + item.getProductId()));

                if (inventory.getAvailableQuantity() < item.getQuantity()) {
                    // Stok yetersiz - başarısız event gönder
                    sendFailureEvent(event.getOrderId(),
                            "Yetersiz stok: " + item.getProductName() +
                                    " (Mevcut: " + inventory.getAvailableQuantity() +
                                    ", İstenen: " + item.getQuantity() + ")");
                    return;
                }
            }

            // 2. Rezervasyon oluştur
            Reservation reservation = Reservation.builder()
                    .orderId(event.getOrderId())
                    .status(ReservationStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            reservation = reservationRepository.save(reservation);

            // 3. Her ürün için rezervasyon yap
            for (InventoryReserveRequestEvent.OrderItemEvent item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId()).get();

                // Rezervasyon item oluştur
                ReservationItem reservationItem = ReservationItem.builder()
                        .reservation(reservation)
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build();
                reservationItemRepository.save(reservationItem);

                // Stok güncelle
                inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
                inventoryRepository.save(inventory);

                // Hareket kaydı
                InventoryMovement movement = InventoryMovement.builder()
                        .productId(item.getProductId())
                        .movementType(MovementType.RESERVATION)
                        .quantity(item.getQuantity())
                        .referenceId(reservation.getId())
                        .notes("Sipariş rezervasyonu: " + event.getOrderId())
                        .build();
                movementRepository.save(movement);

                log.debug("Stok rezerve edildi: productId={}, quantity={}",
                        item.getProductId(), item.getQuantity());
            }

            // 4. Rezervasyonu onayla
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setConfirmedAt(LocalDateTime.now());
            reservationRepository.save(reservation);

            // 5. Başarılı event gönder
            sendSuccessEvent(event.getOrderId(), reservation.getId());

            log.info("Stok rezervasyonu tamamlandı: orderId={}, reservationId={}",
                    event.getOrderId(), reservation.getId());

        } catch (Exception e) {
            log.error("Stok rezervasyonu başarısız: orderId={}, error={}",
                    event.getOrderId(), e.getMessage());
            sendFailureEvent(event.getOrderId(), "Sistem hatası: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void releaseInventoryForOrder(UUID orderId, String reason) {
        log.info("Sipariş için stok serbest bırakılıyor: orderId={}, reason={}", orderId, reason);

        reservationRepository.findByOrderId(orderId).ifPresent(reservation -> {
            if (reservation.getStatus() == ReservationStatus.CONFIRMED ||
                    reservation.getStatus() == ReservationStatus.PENDING) {

                // Her item için stok geri ver
                for (ReservationItem item : reservation.getItems()) {
                    Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                            .orElse(null);

                    if (inventory != null) {
                        inventory.setReservedQuantity(
                                inventory.getReservedQuantity() - item.getQuantity());
                        inventoryRepository.save(inventory);

                        // Hareket kaydı
                        InventoryMovement movement = InventoryMovement.builder()
                                .productId(item.getProductId())
                                .movementType(MovementType.RESERVATION_CANCEL)
                                .quantity(item.getQuantity())
                                .referenceId(reservation.getId())
                                .notes("Rezervasyon iptali: " + reason)
                                .build();
                        movementRepository.save(movement);
                    }
                }

                // Rezervasyonu iptal et
                reservation.setStatus(ReservationStatus.RELEASED);
                reservation.setReleasedAt(LocalDateTime.now());
                reservationRepository.save(reservation);

                log.info("Stok serbest bırakıldı: orderId={}", orderId);
            }
        });
    }

    private void sendSuccessEvent(UUID orderId, UUID reservationId) {
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .reservationId(reservationId)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.sendInventoryReservedEvent(event);
    }

    private void sendFailureEvent(UUID orderId, String reason) {
        InventoryReservedEvent event = InventoryReservedEvent.builder()
                .orderId(orderId)
                .success(false)
                .failureReason(reason)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaProducer.sendInventoryReservedEvent(event);
    }

    // ==================== HELPER METHODS ====================

    private void createMovement(UUID productId, MovementType type, Integer quantity,
                                UUID referenceId, String referenceType, String notes) {
        InventoryMovement movement = InventoryMovement.builder()
                .productId(productId)
                .movementType(type)
                .quantity(quantity)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .notes(notes)
                .build();
        movementRepository.save(movement);
    }
}