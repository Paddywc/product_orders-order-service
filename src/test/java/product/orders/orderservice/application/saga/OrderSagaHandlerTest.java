package product.orders.orderservice.application.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import product.orders.orderservice.application.OrderApplicationService;
import product.orders.orderservice.domain.exception.OrderNotFoundException;
import product.orders.orderservice.messaging.event.*;
import product.orders.orderservice.messaging.producer.OrderEventProducer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrderSagaHandlerTest {


    private OrderApplicationService orderApplicationService;
    private OrderEventProducer orderEventProducer;
    private OrderSagaHandler handler;

    @BeforeEach
    void setUp() {
        orderApplicationService = mock(OrderApplicationService.class);
        orderEventProducer = mock(OrderEventProducer.class);
        handler = new OrderSagaHandler(orderApplicationService, orderEventProducer);
    }

    @Test
    void testMarkInventoryReserved_WhenOrderConfirmed_PublishesOrderConfirmedEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(UUID.randomUUID(), orderId, Instant.now());

        ArgumentCaptor<OrderConfirmedEvent> eventCaptor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);

        when(orderApplicationService.markOrderInventoryReserved(orderId))
                .thenReturn(true);

        // Act
        handler.markInventoryReserved(event);

        // Assert
        verify(orderApplicationService).markOrderInventoryReserved(orderId);
        verify(orderEventProducer).publish(eventCaptor.capture());
        assertEquals(orderId, eventCaptor.getValue().orderId());
    }

    @Test
    void testMarkInventoryReserved_WhenNotConfirmed_DoesNotPublishEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(UUID.randomUUID(), orderId, Instant.now());

        when(orderApplicationService.markOrderInventoryReserved(orderId))
                .thenReturn(false);

        // Act
        handler.markInventoryReserved(event);

        // Assert
        verify(orderApplicationService).markOrderInventoryReserved(orderId);
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void markInventoryReserved_whenOrderNotFound_publishesOrderCancelled() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(UUID.randomUUID(), orderId, Instant.now());

        when(orderApplicationService.markOrderInventoryReserved(orderId))
                .thenThrow(OrderNotFoundException.class);

        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);

        // Act
        handler.markInventoryReserved(event);

        // Assert
        verify(orderEventProducer).publish(eventCaptor.capture());
        assertEquals(orderId, eventCaptor.getValue().orderId());
        assertEquals(CancellationReason.ORDER_NOT_FOUND, eventCaptor.getValue().reason());
    }

    @Test
    void markInventoryReserved_whenInvalidState_DoesNothing() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(UUID.randomUUID(), orderId, Instant.now());

        when(orderApplicationService.markOrderInventoryReserved(orderId))
                .thenThrow(new IllegalStateException("invalid"));

        // Act
        handler.markInventoryReserved(event);

        // Assert
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void testMarkInventoryReservationFailed_SendOrderInCreatedState_CancelsOrderAndPublishesCancelledEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                UUID.randomUUID(),
                orderId,
                InventoryReservationFailedReason.DUPLICATE_RESERVATION,
                "Failed",
                Instant.now());

        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);

        // Act
        handler.markInventoryReservationFailed(event);

        // Assert
        verify(orderApplicationService).markInventoryReservationFailedAndCancel(orderId);
        verify(orderEventProducer).publish(eventCaptor.capture());
        assertEquals(orderId, eventCaptor.getValue().orderId());
        assertEquals(CancellationReason.INVENTORY_RESERVATION_FAILED, eventCaptor.getValue().reason());
    }

    @Test
    void TestMarkInventoryReservationFailed_WhenOrderNotFound_PublishesOrderCancelledWithOrderNotFoundReason() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                UUID.randomUUID(),
                orderId,
                InventoryReservationFailedReason.DUPLICATE_RESERVATION,
                "Failed",
                Instant.now());

        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        doThrow(OrderNotFoundException.class)
                .when(orderApplicationService).markInventoryReservationFailedAndCancel(orderId);
        // Act
        handler.markInventoryReservationFailed(event);


        // Assert
        verify(orderEventProducer).publish(eventCaptor.capture());
        assertEquals(orderId, eventCaptor.getValue().orderId());
        assertEquals(CancellationReason.ORDER_NOT_FOUND, eventCaptor.getValue().reason());
    }

    @Test
    void TestMarkInventoryReservationFailed_WhenOrderAlreadyConfirmed_DoesNothing() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                UUID.randomUUID(),
                orderId,
                InventoryReservationFailedReason.DUPLICATE_RESERVATION,
                "Failed",
                Instant.now());

        doThrow(new IllegalStateException("already confirmed"))
                .when(orderApplicationService).markInventoryReservationFailedAndCancel(orderId);

        // Act
        handler.markInventoryReservationFailed(event);

        // Assert
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void TestMarkPaymentComplete_WhenOrderConfirmed_PublishesOrderConfirmed() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                100L,
                "USD",
                Instant.now()
        );

        when(orderApplicationService.markOrderPaymentComplete(orderId))
                .thenReturn(true);
        ArgumentCaptor<OrderConfirmedEvent> publishedEventCaptor = ArgumentCaptor.forClass(OrderConfirmedEvent.class);

        // Act
        handler.markPaymentComplete(event);

        // Assert
        verify(orderEventProducer).publish(publishedEventCaptor.capture());
        assertEquals(orderId, publishedEventCaptor.getValue().orderId());
    }

    @Test
    void TestMarkPaymentComplete_WhenOrderNotConfirmed_DoesNotPublishEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                orderId,
                100L,
                "USD",
                Instant.now()
        );

        when(orderApplicationService.markOrderPaymentComplete(orderId))
                .thenReturn(false);

        // Act
        handler.markPaymentComplete(event);

        // Assert
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void testMarkPaymentComplete_WhenOrderNotFound_PublishesOrderCancelled() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                200L,
                "EUR",
                Instant.now()
        );

        when(orderApplicationService.markOrderPaymentComplete(orderId))
                .thenThrow(OrderNotFoundException.class);
        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);

        // Act
        handler.markPaymentComplete(event);

        // Assert
        verify(orderEventProducer).publish(eventCaptor.capture());
        assertEquals(orderId, eventCaptor.getValue().orderId());
        assertEquals(CancellationReason.ORDER_NOT_FOUND, eventCaptor.getValue().reason());
    }

    @Test
    void testMarkPaymentComplete_WhenOrderNotInCreatedState_DoesNothing() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                orderId,
                200L,
                "EUR",
                Instant.now()
        );

        when(orderApplicationService.markOrderPaymentComplete(orderId))
                .thenThrow(IllegalStateException.class);

        // Act
        handler.markPaymentComplete(event);

        // Assert
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    void testMarkPaymentFailed_AndCancel_SentValidData_CancelsOrderAndFiresOrderCancelledEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                orderId,
                "Invalid payment method",
                Instant.now()
        );

        ArgumentCaptor<OrderCancelledEvent> publishedEventCapture = ArgumentCaptor.forClass(OrderCancelledEvent.class);

        // Act
        handler.markPaymentFailed(event);

        // Assert
        verify(orderEventProducer).publish(publishedEventCapture.capture());
        assertEquals(orderId, publishedEventCapture.getValue().orderId());
        assertEquals(CancellationReason.PAYMENT_FAILED, publishedEventCapture.getValue().reason());
    }

    @Test
    void testMarkPaymentFailed_AndCancel_OrderIsConfirmed_DoesNothing() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                orderId,
                "Invalid payment method",
                Instant.now()
        );

        doThrow(new IllegalStateException())
                .when(orderApplicationService)
                .markPaymentFailedAndCancel(orderId);

        // Act
        handler.markPaymentFailed(event);

        // Assert
        verifyNoInteractions(orderEventProducer);
    }

}