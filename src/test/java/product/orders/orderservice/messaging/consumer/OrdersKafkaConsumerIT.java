package product.orders.orderservice.messaging.consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.messaging.event.*;
import product.orders.orderservice.repository.OrderRepository;
import product.orders.orderservice.repository.ProcessedInventoryEventRepository;
import product.orders.orderservice.repository.ProcessedPaymentEventRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrdersKafkaConsumerIT {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;


    @Autowired
    KafkaTopicsProperties topics;

    @Autowired
    OrderApplicationService orderApplicationService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProcessedPaymentEventRepository processedPaymentEventRepository;

    @Autowired
    ProcessedInventoryEventRepository processedInventoryEventRepository;


    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
            );

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("inventory_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers);

        registry.add("spring.datasource.url",
                mysql::getJdbcUrl);

        registry.add("spring.datasource.username",
                mysql::getUsername);

        registry.add("spring.datasource.password",
                mysql::getPassword);
    }

    static {
        kafka.start();
        mysql.start();
    }


    @DynamicPropertySource
    static void overrideKafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);


        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @AfterAll
    void tearDown() {
        if (kafka != null) {
            kafka.stop();
        }
        if (mysql != null) {
            mysql.stop();
        }
    }

    private CustomerDetails createCustomerDetails(UUID customerId) {
        return new CustomerDetails(customerId, "anemail@gmail.com", "123 Fake Street, Springfield, USA");
    }


    @Test
    void testPaymentCompleted_InventoryAlreadyComplete_OrderIsConfirmedAndEventIsMarkedProcessed() {
        // Arrange
        // create an order in DB
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Money totalAmount = new Money(100L, "USD");
        List<OrderItem> items = List.of(new OrderItem(productId, "Test name", 1, totalAmount));
        UUID orderId = orderApplicationService.createOrder(createCustomerDetails(customerId), items, totalAmount);

        // Ensure order is one step away from confirmation (inventory reserved)
        orderApplicationService.markOrderInventoryReserved(orderId);

        UUID eventId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId,
                orderId,
                UUID.randomUUID(),
                totalAmount.getAmountCents(),
                totalAmount.getCurrency(),
                Instant.now()
        );
        Message<PaymentCompletedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader(KafkaHeaders.TOPIC, topics.getPaymentEvents())
                .setHeader("eventType", "PaymentCompletedEvent")
                .build();

        // Act
        // publish the event to Kafka (the app's listener should consume it)
        kafkaTemplate.executeInTransaction(kt -> {
            kafkaTemplate.send(message);
            kafkaTemplate.flush();
            return null;
        });

        // Assert
        // order confirmed + idempotency marker persisted
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

            assertThat(processedPaymentEventRepository.existsByEventId(eventId)).isTrue();
        });
    }

    @Test
    void testPaymentFailed_EventConsumed_OrderIsCanceledAndEventIsMarkedProcessed() {
        // Arrange
        // create an order in DB
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Money totalAmount = new Money(100L, "USD");
        List<OrderItem> items = List.of(new OrderItem(productId, "Test name", 1, totalAmount));
        UUID orderId = orderApplicationService.createOrder(createCustomerDetails(customerId), items, totalAmount);

        UUID eventId = UUID.randomUUID();
        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                orderId,
                "Insufficient funds",
                Instant.now()
        );
        Message<PaymentFailedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader(KafkaHeaders.TOPIC, topics.getPaymentEvents())
                .setHeader("eventType", "PaymentFailedEvent")
                .build();

        // Act
        // publish the event to Kafka (the app's listener should consume it)
        kafkaTemplate.executeInTransaction(kt -> {
            kafkaTemplate.send(message);
            kafkaTemplate.flush();
            return null; // return value is ignored
        });


        // Assert
        // side effects (DB updated + idempotency marker persisted)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            assertThat(processedPaymentEventRepository.existsByEventId(eventId)).isTrue();
        });
    }


    @Test
    void testInventoryReserved_EventConsumed_OrderIsConfirmedAndEventIsMarkedProcessed() {
        // Arrange: create an order in DB
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Money totalAmount = new Money(100L, "USD");
        List<OrderItem> items = List.of(new OrderItem(productId, "Test name", 1, totalAmount));
        UUID orderId = orderApplicationService.createOrder(createCustomerDetails(customerId), items, totalAmount);

        // Ensure order is one step away from confirmation (payment completed)
        orderApplicationService.markOrderPaymentComplete(orderId);

        UUID eventId = UUID.randomUUID();
        InventoryReservedEvent event = new InventoryReservedEvent(
                eventId,
                orderId,
                Instant.now()
        );
        Message<InventoryReservedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader(KafkaHeaders.TOPIC, topics.getInventoryEvents())
                .setHeader("eventType", "InventoryReservedEvent")
                .build();

        // Act
        // publish the event to Kafka (the app's listener should consume it)
        kafkaTemplate.executeInTransaction(kt -> {
            kafkaTemplate.send(message);
            kafkaTemplate.flush();
            return null;
        });

        // Assert
        // order confirmed + idempotency marker persisted
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

            assertThat(processedInventoryEventRepository.existsByEventId(eventId)).isTrue();
        });
    }

    @Test
    void testInventoryReservationFailed_EventConsumed_OrderIsCanceledAndEventIsMarkedProcessed() {
        // Arrange
        // create an order in DB
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Money totalAmount = new Money(100L, "USD");
        List<OrderItem> items = List.of(new OrderItem(productId, "Test name", 1, totalAmount));
        UUID orderId = orderApplicationService.createOrder(createCustomerDetails(customerId), items, totalAmount);

        UUID eventId = UUID.randomUUID();
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                eventId,
                orderId,
                InventoryReservationFailedReason.INSUFFICIENT_INVENTORY,
                "Not enough stock to fulfill the order",
                Instant.now()
        );
        Message<InventoryReservationFailedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.KEY, orderId.toString())
                .setHeader(KafkaHeaders.TOPIC, topics.getInventoryEvents())
                .setHeader("eventType", "InventoryReservationFailedEvent")
                .build();

        // Act
        // publish the event to Kafka (the app's listener should consume it)
        kafkaTemplate.executeInTransaction(kt -> {
            kafkaTemplate.send(message);
            kafkaTemplate.flush();
            return null;
        });

        // Assert
        // side effects (DB updated + idempotency marker persisted)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            var order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            assertThat(processedInventoryEventRepository.existsByEventId(eventId)).isTrue();
        });
    }

}

