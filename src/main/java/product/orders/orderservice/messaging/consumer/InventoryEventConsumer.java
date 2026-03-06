package product.orders.orderservice.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import product.orders.orderservice.application.saga.OrderSagaHandler;
import product.orders.orderservice.messaging.event.InventoryReleasedEvent;
import product.orders.orderservice.messaging.event.InventoryReservationFailedEvent;
import product.orders.orderservice.messaging.event.InventoryReservedEvent;
import product.orders.orderservice.persistance.ProcessedInventoryEvent;
import product.orders.orderservice.repository.ProcessedInventoryEventRepository;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Consumes events from the inventory service
 */
@Component
public class InventoryEventConsumer extends EventConsumer<ProcessedInventoryEvent> {

    public InventoryEventConsumer(OrderSagaHandler sagaHandler, ProcessedInventoryEventRepository processedInventoryEventRepository) {
        super(sagaHandler, processedInventoryEventRepository, ProcessedInventoryEvent::new);
    }

    @KafkaListener(topics = "#{@kafkaTopicsProperties.inventoryEvents}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleEvent(String rawJson,
                            @Header("eventType") String eventType) {
        JsonNode node = extractJson(rawJson);
        String eventIdKey = node.get("eventId").stringValue();

        if (eventAlreadyProcessed(UUID.fromString(eventIdKey))) return;
        switch (eventType) {
            case "InventoryReservedEvent" ->
                    sagaHandler.markInventoryReserved(objectMapper.treeToValue(node, InventoryReservedEvent.class));
            case "InventoryReservationFailedEvent" ->
                    sagaHandler.markInventoryReservationFailed(objectMapper.treeToValue(node, InventoryReservationFailedEvent.class));
            case "InventoryReleasedEvent" ->
                    sagaHandler.markInventoryReleased(objectMapper.treeToValue(node, InventoryReleasedEvent.class));
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        }
    }


}

