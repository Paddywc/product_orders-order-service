package product.orders.orderservice.messaging.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import product.orders.orderservice.messaging.event.InvalidPaymentMadeEvent;
import product.orders.orderservice.messaging.event.OrderCancelledEvent;
import product.orders.orderservice.messaging.event.OrderConfirmedEvent;
import product.orders.orderservice.messaging.event.OrderCreatedEvent;
import product.orders.orderservice.config.KafkaTopicsProperties;

import java.util.UUID;

/**
 * Produces Kafka messages for Order events
 */
@Component
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private KafkaTopicsProperties topics;


    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    private String getTopicName() {
        return topics.getOrderEvents();
    }

    private <T>Message<T> buildMessage(T event, UUID orderId, String eventType){
        return MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, getTopicName())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader("eventType", eventType)
                .build();
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(OrderCreatedEvent event) {
        Message<OrderCreatedEvent> message = buildMessage(event, event.orderId(), "OrderCreatedEvent");
        kafkaTemplate.send(message);
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(OrderConfirmedEvent event) {
        Message<OrderConfirmedEvent> message = buildMessage(event, event.orderId(), "OrderConfirmedEvent");
        kafkaTemplate.send(message);
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(OrderCancelledEvent event) {
        Message<OrderCancelledEvent> message = buildMessage(event, event.orderId(), "OrderCancelledEvent");
        kafkaTemplate.send(message);
    }

    @Transactional(value = "kafkaTransactionManager")
    public void publish(InvalidPaymentMadeEvent event){
        Message<InvalidPaymentMadeEvent> message = buildMessage(event, event.orderId(), "InvalidPaymentMadeEvent");
        kafkaTemplate.send(message);
    }


}
