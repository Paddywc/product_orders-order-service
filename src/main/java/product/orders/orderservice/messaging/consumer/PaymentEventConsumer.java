package product.orders.orderservice.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import product.orders.orderservice.application.saga.OrderSagaHandler;
import product.orders.orderservice.messaging.event.PaymentCompletedEvent;
import product.orders.orderservice.messaging.event.PaymentFailedEvent;
import product.orders.orderservice.messaging.event.PaymentRefundedEvent;
import product.orders.orderservice.persistance.ProcessedPaymentEvent;
import product.orders.orderservice.repository.ProcessedPaymentEventRepository;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Consumes events from the payment service
 */
@Component
public class PaymentEventConsumer extends EventConsumer<ProcessedPaymentEvent> {

    public PaymentEventConsumer(OrderSagaHandler sagaHandler, ProcessedPaymentEventRepository processedPaymentEventRepository) {
        super(sagaHandler, processedPaymentEventRepository, ProcessedPaymentEvent::new);
    }

    @KafkaListener(topics = "#{@kafkaTopicsProperties.paymentEvents}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleEvent(String rawJson,
                            @Header("eventType") String eventType) {
        JsonNode node = extractJson(rawJson);
        String eventIdKey = node.get("eventId").stringValue();

        if (eventAlreadyProcessed(UUID.fromString(eventIdKey))) return;

        switch (eventType) {
            case "PaymentCompletedEvent" ->
                    sagaHandler.markPaymentComplete(objectMapper.treeToValue(node, PaymentCompletedEvent.class));
            case "PaymentFailedEvent" ->
                    sagaHandler.markPaymentFailed(objectMapper.treeToValue(node, PaymentFailedEvent.class));
            case "PaymentRefundedEvent" ->
                    sagaHandler.markPaymentRefunded(objectMapper.treeToValue(node, PaymentRefundedEvent.class));
        }
    }


}

