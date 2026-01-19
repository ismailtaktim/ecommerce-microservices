package com.ecommerce.order.service;

import com.ecommerce.order.dto.request.*;
import com.ecommerce.order.dto.response.*;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.event.*;
import com.ecommerce.order.exception.*;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SagaStateRepository sagaStateRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final OrderMapper orderMapper;
    private final OutboxService outboxService;

    // Kafka Topic isimleri
    private static final String TOPIC_INVENTORY_RESERVE = "inventory-reserve-request";
    private static final String TOPIC_INVENTORY_RELEASE = "inventory-release-request";
    private static final String TOPIC_PAYMENT_REQUEST = "payment-request";
    private static final String TOPIC_PAYMENT_REFUND = "payment-refund-request";
    private static final String TOPIC_ORDER_CREATED = "order-created";
    private static final String TOPIC_ORDER_COMPLETED = "order-completed";
    private static final String TOPIC_ORDER_CANCELLED = "order-cancelled";

    // ==================== ORDER CRUD ====================

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Sipariş oluşturuluyor: CustomerId={}", request.getCustomerId());

        // 1. Sipariş oluştur
        Order order = buildOrder(request);
        order = orderRepository.save(order);

        // 2. Sipariş kalemlerini ekle
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = buildOrderItem(order, itemRequest);
            order.addItem(item);
            subtotal = subtotal.add(item.getTotalPrice());
        }

        // 3. Toplam hesapla
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.18")); // %18 KDV
        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(subtotal.add(taxAmount));

        order = orderRepository.save(order);

        // 4. Saga başlat
        startSaga(order);

        // 5. Durum geçmişine ekle
        saveStatusHistory(order, null, OrderStatus.PENDING, "Sipariş oluşturuldu");

        // Order created event gönder (Notification Service için)
        publishOrderCreatedEvent(order);

        log.info("Sipariş oluşturuldu: OrderNumber={}", order.getOrderNumber());
        return orderMapper.toResponse(order);
    }

    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .totalAmount(order.getTotalAmount())
                .createdAt(LocalDateTime.now())
                .build();

        outboxService.saveEvent("ORDER", order.getId(), TOPIC_ORDER_CREATED, event);
        log.info("Order created event gönderildi: {}", order.getOrderNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = findOrderById(orderId);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: " + orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::toListResponse);
    }

    // ==================== ORDER OPERATIONS ====================

    @Override
    public OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request) {
        Order order = findOrderById(orderId);

        // İptal edilebilir durumda mı?
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Tamamlanmış sipariş iptal edilemez");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Sipariş zaten iptal edilmiş");
        }

        OrderStatus oldStatus = order.getStatus();

        // Duruma göre compensation işlemleri
        if (oldStatus == OrderStatus.PAYMENT_COMPLETED || oldStatus == OrderStatus.INVENTORY_RESERVED) {
            // Ödeme yapıldıysa iade et
            if (oldStatus == OrderStatus.PAYMENT_COMPLETED) {
                requestPaymentRefund(order);
            }
            // Stok rezerve edildiyse serbest bırak
            requestInventoryRelease(order, request.getReason());
        }

        // Siparişi iptal et
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getReason());
        order.setCancelledBy(request.getCancelledBy());
        order = orderRepository.save(order);

        // Saga durumunu güncelle
        updateSagaStatus(order.getId(), SagaStatus.FAILED, "ORDER_CANCELLED");

        // Durum geçmişi
        saveStatusHistory(order, oldStatus, OrderStatus.CANCELLED, request.getReason());

        // Event yayınla
        publishOrderCancelledEvent(order, request.getReason());

        log.info("Sipariş iptal edildi: OrderNumber={}, Reason={}", order.getOrderNumber(), request.getReason());
        return orderMapper.toResponse(order);
    }

    // ==================== SAGA CALLBACKS ====================

    @Override
    public void handleInventoryReserved(UUID orderId, boolean success, String failureReason) {
        Order order = findOrderById(orderId);

        if (success) {
            log.info("Stok rezerve edildi: OrderId={}", orderId);

            // Durumu güncelle
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(OrderStatus.INVENTORY_RESERVED);
            orderRepository.save(order);

            // Saga durumunu güncelle
            updateSagaStatus(orderId, SagaStatus.INVENTORY_RESERVED, "INVENTORY_RESERVED");

            // Durum geçmişi
            saveStatusHistory(order, oldStatus, OrderStatus.INVENTORY_RESERVED, "Stok rezerve edildi");

            // Sonraki adım: Ödeme iste
            requestPayment(order);

        } else {
            log.error("Stok rezerve edilemedi: OrderId={}, Reason={}", orderId, failureReason);

            // Siparişi başarısız yap
            failOrder(order, failureReason);
        }
    }

    @Override
    public void handlePaymentCompleted(UUID orderId, boolean success, String failureReason) {
        Order order = findOrderById(orderId);

        if (success) {
            log.info("Ödeme tamamlandı: OrderId={}", orderId);

            // Durumu güncelle
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(OrderStatus.PAYMENT_COMPLETED);
            orderRepository.save(order);

            // Saga durumunu güncelle
            updateSagaStatus(orderId, SagaStatus.PAYMENT_COMPLETED, "PAYMENT_COMPLETED");

            // Durum geçmişi
            saveStatusHistory(order, oldStatus, OrderStatus.PAYMENT_COMPLETED, "Ödeme alındı");

            // Siparişi tamamla
            completeOrder(order);

        } else {
            log.error("Ödeme başarısız: OrderId={}, Reason={}", orderId, failureReason);

            // Stok rezervasyonunu serbest bırak (compensation)
            requestInventoryRelease(order, "Ödeme başarısız: " + failureReason);

            // Siparişi başarısız yap
            failOrder(order, failureReason);
        }
    }

    // ==================== SAGA HELPER METHODS ====================

    private void startSaga(Order order) {
        // Saga state oluştur
        SagaState saga = SagaState.builder()
                .orderId(order.getId())
                .status(SagaStatus.STARTED)
                .currentStep("ORDER_CREATED")
                .build();
        sagaStateRepository.save(saga);

        // İlk adım: Stok rezervasyonu iste
        requestInventoryReservation(order);

        // Saga durumunu güncelle
        updateSagaStatus(order.getId(), SagaStatus.INVENTORY_PENDING, "INVENTORY_PENDING");
    }

    private void requestInventoryReservation(Order order) {
        List<OrderItemEvent> items = order.getItems().stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        InventoryReserveRequestEvent event = InventoryReserveRequestEvent.builder()
                .orderId(order.getId())
                .items(items)
                .build();

        outboxService.saveEvent("ORDER", order.getId(), TOPIC_INVENTORY_RESERVE, event);
        log.info("Stok rezervasyon isteği gönderildi: OrderId={}", order.getId());
    }

    private void requestInventoryRelease(Order order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .totalAmount(order.getTotalAmount())
                .reason(reason)
                .cancelledAt(LocalDateTime.now())
                .build();

        outboxService.saveEvent("ORDER", order.getId(), TOPIC_INVENTORY_RELEASE, event);
        log.info("Stok serbest bırakma isteği gönderildi: OrderId={}", order.getId());
    }

    private void requestPayment(Order order) {
        // Saga durumunu güncelle
        updateSagaStatus(order.getId(), SagaStatus.PAYMENT_PENDING, "PAYMENT_PENDING");

        PaymentRequestEvent event = PaymentRequestEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();

        outboxService.saveEvent("ORDER", order.getId(), TOPIC_PAYMENT_REQUEST, event);
        log.info("Ödeme isteği gönderildi: OrderId={}, Amount={}", order.getId(), order.getTotalAmount());
    }

    private void requestPaymentRefund(Order order) {
        PaymentRequestEvent event = PaymentRequestEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();

        outboxService.saveEvent("ORDER", order.getId(), TOPIC_PAYMENT_REFUND, event);
        log.info("İade isteği gönderildi: OrderId={}", order.getId());
    }

    private void completeOrder(Order order) {
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        // Saga tamamlandı
        SagaState saga = sagaStateRepository.findByOrderId(order.getId()).orElse(null);
        if (saga != null) {
            saga.setStatus(SagaStatus.COMPLETED);
            saga.setCurrentStep("COMPLETED");
            saga.setCompletedAt(LocalDateTime.now());
            sagaStateRepository.save(saga);
        }

        // Durum geçmişi
        saveStatusHistory(order, oldStatus, OrderStatus.COMPLETED, "Sipariş tamamlandı");

        // Event yayınla
        publishOrderCompletedEvent(order);

        log.info("Sipariş tamamlandı: OrderNumber={}", order.getOrderNumber());
    }

    private void failOrder(Order order, String reason) {
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        orderRepository.save(order);

        // Saga başarısız
        updateSagaStatus(order.getId(), SagaStatus.FAILED, "FAILED");

        // Durum geçmişi
        saveStatusHistory(order, oldStatus, OrderStatus.FAILED, reason);

        log.error("Sipariş başarısız: OrderNumber={}, Reason={}", order.getOrderNumber(), reason);
    }

    private void updateSagaStatus(UUID orderId, SagaStatus status, String step) {
        sagaStateRepository.findByOrderId(orderId).ifPresent(saga -> {
            saga.setStatus(status);
            saga.setCurrentStep(step);
            sagaStateRepository.save(saga);
        });
    }

    // ==================== EVENT PUBLISHERS ====================

    private void publishOrderCompletedEvent(Order order) {
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .totalAmount(order.getTotalAmount())
                .completedAt(LocalDateTime.now())
                .build();

        outboxService.saveEvent("ORDER", order.getId(), TOPIC_ORDER_COMPLETED, event);
        log.info("Order completed event gönderildi: {}", order.getOrderNumber());
    }

    private void publishOrderCancelledEvent(Order order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .totalAmount(order.getTotalAmount())
                .reason(reason)
                .cancelledAt(LocalDateTime.now())
                .build();

        outboxService.saveEvent("ORDER", order.getId(), TOPIC_ORDER_CANCELLED, event);
    }

    // ==================== HELPER METHODS ====================

    private Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı"));
    }

    private Order buildOrder(CreateOrderRequest request) {
        ShippingAddressRequest address = request.getShippingAddress();

        return Order.builder()
                .orderNumber(generateOrderNumber()) // Bu satırı ekle
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .status(OrderStatus.PENDING)
                .shippingRecipientName(address.getRecipientName())
                .shippingPhone(address.getPhone())
                .shippingAddressLine1(address.getAddressLine1())
                .shippingAddressLine2(address.getAddressLine2())
                .shippingDistrict(address.getDistrict())
                .shippingCity(address.getCity())
                .shippingPostalCode(address.getPostalCode())
                .shippingCountry(address.getCountry())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .currency("TRY")
                .build();
    }

    // Bu metodu da ekle (class içinde herhangi bir yere)
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private OrderItem buildOrderItem(Order order, OrderItemRequest request) {
        BigDecimal totalPrice = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        return OrderItem.builder()
                .order(order)
                .productId(request.getProductId())
                .productName(request.getProductName())
                .productSku(request.getProductSku())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .totalPrice(totalPrice)
                .build();
    }

    private void saveStatusHistory(Order order, OrderStatus oldStatus, OrderStatus newStatus, String reason) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();
        statusHistoryRepository.save(history);
    }
}