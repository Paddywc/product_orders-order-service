package product.orders.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registry of Kafka topics used in the Inventory Service. Creates a string bean kafkaTopicsProperties
 */
@Configuration
@ConfigurationProperties(prefix = "kafka.topic")
public class KafkaTopicsProperties {

    /**
     * The name of the topic that stores the order service events
     */
    private String orderEvents;
    /**
     * The name of the topic that stores the inventory service events
     */
    private String inventoryEvents;

    /**
     * The name of the topic that stores the payment service events
     */
    private String paymentEvent;

    public String getOrderEvents() {
        return orderEvents;
    }


    public String getInventoryEvents() {
        return inventoryEvents;
    }

    public String getPaymentEvent() {
        return paymentEvent;
    }
}
