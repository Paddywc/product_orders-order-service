package product.orders.orderservice.messaging.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import product.orders.orderservice.application.saga.OrderSagaHandler;
import product.orders.orderservice.messaging.event.InventoryReservationFailedEvent;
import product.orders.orderservice.messaging.event.InventoryReservationFailedReason;
import product.orders.orderservice.messaging.event.InventoryReservedEvent;
import product.orders.orderservice.persistance.ProcessedInventoryEvent;
import product.orders.orderservice.repository.ProcessedInventoryEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the InventoryEventConsumer class.
 * This class handles Kafka messages related to inventory events and marks them as processed.
 */
@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @InjectMocks
    private InventoryEventConsumer inventoryEventConsumer;

    @Mock
    private ProcessedInventoryEventRepository processedInventoryEventRepository;

    @Mock
    private OrderSagaHandler orderSagaHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testHandleInventoryReservedEvent_WhenEventNotProcessed_ShouldMarkAsReserved() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryReservedEvent event = new InventoryReservedEvent(eventId, orderId, occurredAt);
        when(processedInventoryEventRepository.saveAndFlush(any(ProcessedInventoryEvent.class))).thenReturn(new ProcessedInventoryEvent(eventId));

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(eventAsString, "InventoryReservedEvent");

        // Assert
        verify(orderSagaHandler, times(1)).markInventoryReserved(event);
    }

    @Test
    void testHandleInventoryReservedEvent_WhenEventAlreadyProcessed_ShouldDoNothing() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryReservedEvent event = new InventoryReservedEvent(eventId, orderId, occurredAt);
        when(processedInventoryEventRepository.saveAndFlush(any(ProcessedInventoryEvent.class))).thenThrow(
                DataIntegrityViolationException.class
        );
        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(eventAsString, "InventoryReservedEvent");

        // Assert
        verify(orderSagaHandler, times(0)).markInventoryReserved(event);
    }

    @Test
    void handleInventoryReservationFailedEvent_WhenEventNotProcessed_ShouldMarkAsFailed() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                eventId,
                orderId,
                InventoryReservationFailedReason.DUPLICATE_RESERVATION,
                "Duplicate reservation detected",
                occurredAt);
        when(processedInventoryEventRepository.saveAndFlush(any(ProcessedInventoryEvent.class))).thenReturn(new ProcessedInventoryEvent(eventId));

        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(eventAsString, "InventoryReservationFailedEvent");


        // Assert
        verify(orderSagaHandler, times(1)).markInventoryReservationFailed(event);
    }

    @Test
    void handleInventoryReservationFailedEvent_WhenEventAlreadyProcessed_ShouldDoNothing() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                eventId,
                orderId,
                InventoryReservationFailedReason.DUPLICATE_RESERVATION,
                "Duplicate reservation detected",
                occurredAt);
        when(processedInventoryEventRepository.saveAndFlush(any(ProcessedInventoryEvent.class))).thenThrow(
                DataIntegrityViolationException.class
        );
        // Act
        String eventAsString = objectMapper.writeValueAsString(event);
        inventoryEventConsumer.handleEvent(eventAsString, "InventoryReservationFailedEvent");

        // Assert
        verify(orderSagaHandler, times(0)).markInventoryReservationFailed(event);
    }
}