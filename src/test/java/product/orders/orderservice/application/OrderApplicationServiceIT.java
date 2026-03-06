package product.orders.orderservice.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import product.orders.orderservice.domain.model.CustomerDetails;
import product.orders.orderservice.domain.model.InventoryStatus;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderItem;
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.domain.model.PaymentStatus;
import product.orders.orderservice.messaging.event.InventoryReleasedEvent;
import product.orders.orderservice.messaging.event.OrderCreatedEvent;
import product.orders.orderservice.messaging.producer.OrderEventProducer;
import product.orders.orderservice.repository.OrderItemRepository;
import product.orders.orderservice.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Mock Kafka producer but test real JPA, DB, Flyway migration
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderApplicationServiceIT {

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("orders_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }


    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderApplicationService orderApplicationService;

    // We mock only Kafka producer — everything else is real
    @MockitoBean
    private OrderEventProducer eventProducer;

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void testCreateOrder_PassedValidData_PersistsOrderAndPublishesEvent() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        String customerEmail = "email@example.com";
        String customerAddress = "My fictional address";
        CustomerDetails customerDetails = new CustomerDetails(customerId, customerEmail, customerAddress);

        UUID productId = UUID.randomUUID();
        String productName = "Test product";
        int productQuantity = 1;
        long productPrice = 2000L;
        String currency = "USD";
        Money unitPrice = new Money(productPrice, currency);
        OrderItem orderItem = new OrderItem(productId, productName, productQuantity, unitPrice);

        // Act
        UUID orderId = orderApplicationService.createOrder(customerDetails, List.of(orderItem), unitPrice);

        // Assert
        Order order = orderRepository.findById(orderId).orElseThrow();
        CustomerDetails orderCustomerDetails = order.getCustomerDetails();

        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(orderId);
        assertThat(orderItems.size()).isEqualTo(1);
        OrderItem orderItem1 = orderItems.get(0);

        assertThat(orderCustomerDetails.getCustomerId()).isEqualTo(customerId);
        assertThat(orderCustomerDetails.getCustomerEmail()).isEqualTo(customerEmail);
        assertThat(orderCustomerDetails.getCustomerAddress()).isEqualTo(customerAddress);

        assertThat(orderItem1.getProductId()).isEqualTo(productId);
        assertThat(orderItem1.getProductName()).isEqualTo(productName);
        assertThat(orderItem1.getQuantity()).isEqualTo(productQuantity);
        assertThat(orderItem1.getUnitPrice()).isEqualTo(unitPrice);

        verify(eventProducer).publish(any(OrderCreatedEvent.class));
    }

    @Test
    void testMarkInventoryReservationFailedAndCancel_PassedValidOrderId_CancelsOrder() {
        // Arrange
        UUID orderId = createOrderForCustomer(UUID.randomUUID());

        // Act
        orderApplicationService.markInventoryReservationFailedAndCancel(orderId);

        // Assert
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getInventoryStatus()).isEqualTo(InventoryStatus.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void testMarkPaymentFailedAndCancel_PassedValidOrderId_CancelsOrder() {
        // Arrange
        UUID orderId = createOrderForCustomer(UUID.randomUUID());

        // Act
        orderApplicationService.markPaymentFailedAndCancel(orderId);

        // Assert
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getInventoryStatus()).isEqualTo(InventoryStatus.PENDING);
    }

    @Test
    void testGetOrderStatus_PassedValidOrderId_ReturnsStatus() {
        // Arrange
        UUID orderId = createOrderForCustomer(UUID.randomUUID());

        // Act
        OrderStatus status = orderApplicationService.getOrderStatus(orderId);

        // Assert
        assertThat(status).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void testMarkInventoryReleased_PassedEvent_MarksInventoryReleased() {
        // Arrange
        UUID orderId = createOrderForCustomer(UUID.randomUUID());
        InventoryReleasedEvent event = InventoryReleasedEvent.of(orderId);

        // Act
        orderApplicationService.markInventoryReleased(event);

        // Assert
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getInventoryStatus()).isEqualTo(InventoryStatus.RELEASED);
    }

    @Test
    void testMarkPaymentRefunded_PassedValidOrderId_MarksPaymentRefunded() {
        // Arrange
        UUID orderId = createOrderForCustomer(UUID.randomUUID());

        // Act
        orderApplicationService.markPaymentRefunded(orderId);

        // Assert
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void testGetOrder_PassedValidOrderId_ReturnsOrder() {
        // Arrange
        UUID orderId = createOrderForCustomer(UUID.randomUUID());

        // Act
        Order order = orderApplicationService.getOrder(orderId);

        // Assert
        assertThat(order.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void testGetCustomerOrders_PassedCustomerId_ReturnsCustomerOrders() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID firstOrderId = createOrderForCustomer(customerId);
        UUID secondOrderId = createOrderForCustomer(customerId);

        // Act
        List<Order> orders = orderApplicationService.getCustomerOrders(customerId);

        // Assert
        assertThat(orders.size()).isEqualTo(2);
        assertThat(orders.get(0).getOrderId()).isEqualTo(firstOrderId);
        assertThat(orders.get(1).getOrderId()).isEqualTo(secondOrderId);
    }

    private UUID createOrderForCustomer(UUID customerId) {
        CustomerDetails customerDetails = new CustomerDetails(customerId, "email@example.com", "Any address");
        UUID productId = UUID.randomUUID();
        int productQuantity = 1;
        long productPrice = 2000L;
        Money unitPrice = new Money(productPrice, "USD");
        OrderItem orderItem = new OrderItem(productId, "Test product", productQuantity, unitPrice);
        return orderApplicationService.createOrder(customerDetails, List.of(orderItem), unitPrice);
    }

}
