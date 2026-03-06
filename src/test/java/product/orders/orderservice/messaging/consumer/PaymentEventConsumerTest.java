package product.orders.orderservice.messaging.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import product.orders.orderservice.application.saga.OrderSagaHandler;
import product.orders.orderservice.messaging.event.PaymentCompletedEvent;
import product.orders.orderservice.messaging.event.PaymentFailedEvent;
import product.orders.orderservice.persistance.ProcessedPaymentEvent;
import product.orders.orderservice.repository.ProcessedPaymentEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the PaymentEventConsumer class.
 * This class handles Kafka messages related to inventory events and marks them as processed.
 */
@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @InjectMocks
    private PaymentEventConsumer inventoryEventConsumer;

    @Mock
    private ProcessedPaymentEventRepository processedPaymentEventRepository;

    @Mock
    private OrderSagaHandler orderSagaHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();



    @Test
    void testHandlePaymentCompletedEvent_WhenEventNotProcessed_ShouldMarkAsCompleted() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        PaymentCompletedEvent event = new PaymentCompletedEvent(eventId, UUID.randomUUID(), orderId, 100L, "EUR", occurredAt);
        when(processedPaymentEventRepository.saveAndFlush(any(ProcessedPaymentEvent.class))).thenReturn(new ProcessedPaymentEvent(eventId));

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(eventAsString, "PaymentCompletedEvent");

        // Assert
        verify(orderSagaHandler, times(1)).markPaymentComplete(event);
    }

    @Test
    void testHandlePaymentCompletedEvent_WhenEventAlreadyProcessed_ShouldDoNothing() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        PaymentCompletedEvent event = new PaymentCompletedEvent(eventId, UUID.randomUUID(), orderId,100L, "EUR", occurredAt);
        when(processedPaymentEventRepository.saveAndFlush(any(ProcessedPaymentEvent.class))).thenThrow(
                DataIntegrityViolationException.class
        );
        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(
                eventAsString,
                "PaymentCompletedEvent");

        // Assert
        verify(orderSagaHandler, times(0)).markPaymentComplete(event);
    }

    @Test
    void handlePaymentFailedEvent_WhenEventNotProcessed_ShouldMarkAsFailed() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                orderId,
                "Invalid payment method",
                occurredAt);
        when(processedPaymentEventRepository.saveAndFlush(any(ProcessedPaymentEvent.class))).thenReturn(new ProcessedPaymentEvent(eventId));

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(
                eventAsString,
                "PaymentFailedEvent");


        // Assert
        verify(orderSagaHandler, times(1)).markPaymentFailed(event);
    }

    @Test
    void handlePaymentFailedEvent_WhenEventAlreadyProcessed_ShouldDoNothing() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                orderId,
                "Insufficent funds",
                occurredAt);
        when(processedPaymentEventRepository.saveAndFlush(any(ProcessedPaymentEvent.class))).thenThrow(
                DataIntegrityViolationException.class
        );
        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(eventAsString, "PaymentFailedEvent");

        // Assert
        verify(orderSagaHandler, times(0)).markPaymentFailed(event);
    }
}