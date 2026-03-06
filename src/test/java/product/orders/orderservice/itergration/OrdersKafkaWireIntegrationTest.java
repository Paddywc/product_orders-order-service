package product.orders.orderservice.itergration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import product.orders.orderservice.application.OrderApplicationService;
import product.orders.orderservice.config.KafkaTopicsProperties;
import product.orders.orderservice.domain.model.CustomerDetails;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.OrderItem;
import product.orders.orderservice.messaging.event.PaymentFailedEvent;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext
@Testcontainers
class OrdersKafkaWireIntegrationTest {
    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
            );

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");




    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    ConsumerFactory<String, Object> consumerFactory;

    @Autowired
    KafkaTopicsProperties topics;

    @Autowired
    OrderApplicationService orderApplicationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void overrideKafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @AfterAll
    static void tearDown() {
        if (kafka != null) {
            kafka.stop();
        }
    }

    private UUID createValidOrderAndReturnId() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Long totalAmountPaid = 100L;
        String currency = "USD";
        int quantity = 1;
        Money totalAmount = new Money(totalAmountPaid, currency);
        List<OrderItem> orderItems = List.of(
                new OrderItem(productId, "Test name", quantity, totalAmount)
        );
        CustomerDetails customerDetails = new CustomerDetails(customerId, "email@email.com", "20 Road Ave, Dublin 14, Ireland");

        return orderApplicationService.createOrder(customerDetails, orderItems, totalAmount);
    }

    @Test
    void testConsumer_WhenPaymentFailedEventFires_ReadableConsumerRecord() {
        // Arrange
        UUID orderId = createValidOrderAndReturnId();
        UUID eventId = UUID.randomUUID();
        String paymentFailedReason = "Insufficient funds";
        PaymentFailedEvent event = new PaymentFailedEvent(eventId, orderId, paymentFailedReason, Instant.now());
        Message<PaymentFailedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topics.getPaymentEvents())
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .build();

        // Create a dedicated consumer group for assertions (no competition with app listeners)
        String groupId = "uq-test-consumer-group-" + UUID.randomUUID();
        Consumer<String, Object> consumer = consumerFactory.createConsumer(groupId, UUID.randomUUID().toString());

        consumer.subscribe(List.of(topics.getInventoryEvents(), topics.getPaymentEvents()));

        // Ensure the consumer actually joins the group and gets partition assignments
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            consumer.poll(Duration.ofMillis(100));
            Set<TopicPartition> assignment = consumer.assignment();
            assertThat(assignment.isEmpty()).isFalse();
        });
        consumer.seekToBeginning(consumer.assignment());


        // Act
        // (send within a Kafka transaction if the producer is transaction-capable)
        kafkaTemplate.executeInTransaction(kt -> {
            kafkaTemplate.send(message);
            kafkaTemplate.flush();
            return null; // return value is ignored
        });

        // Assert

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
            assertThat(records.count()).isEqualTo(1);

            ConsumerRecord<String, Object> record = records.iterator().next();
            assertThat(record.key()).isEqualTo(orderId.toString());

            PaymentFailedEvent consumedEvent = objectMapper.readValue(record.value().toString(), PaymentFailedEvent.class);
            assertThat(consumedEvent).isNotNull();
            assertEquals(orderId, consumedEvent.orderId());
            assertEquals(eventId, consumedEvent.eventId());
            assertEquals(paymentFailedReason, consumedEvent.reason());
        });
    }


}

