package product.orders.orderservice.messeging.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import product.orders.orderservice.config.KafkaTopicsProperties;
import product.orders.orderservice.messeging.event.OrderCancelledEvent;
import product.orders.orderservice.messeging.event.OrderConfirmedEvent;
import product.orders.orderservice.messeging.event.OrderCreatedEvent;


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

    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send(
                getTopicName(),
                event.orderId().toString(),
                event
        );
    }

    public void publish(OrderConfirmedEvent event) {
        kafkaTemplate.send(
                getTopicName(),
                event.orderId().toString(),
                event
        );
    }

    public void publish(OrderCancelledEvent event) {
        kafkaTemplate.send(
                getTopicName(),
                event.orderId().toString(),
                event
        );
    }


}
