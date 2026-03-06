package product.orders.orderservice.itergration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import product.orders.orderservice.api.dto.CreateOrderItemRequest;
import product.orders.orderservice.api.dto.CreateOrderRequest;
import product.orders.orderservice.api.dto.CreateOrderResponse;
import product.orders.orderservice.api.dto.GetOrderResponse;
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.domain.model.PaymentStatus;
import product.orders.orderservice.messaging.producer.OrderEventProducer;
import product.orders.orderservice.repository.OrderItemRepository;
import product.orders.orderservice.repository.OrderRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OrderEndToEndIT {

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
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private OrderEventProducer eventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }



    @Test
    void testOrderLifecycle_CreateThenFetchByIdAndCustomer_ReturnsCreatedOrder() throws Exception {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int quantity = 2;
        long unitPriceCents = 500L;
        long totalAmountCents = unitPriceCents * quantity;
        String currency = "USD";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(productId, "Widget", quantity, unitPriceCents)),
                customerId,
                "customer@example.com",
                "123 Example Street",
                totalAmountCents,
                currency
        );

        // Act - create order
        String createResponseBody = mockMvc.perform(post("/orders")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - create response
        CreateOrderResponse created = objectMapper.readValue(createResponseBody, CreateOrderResponse.class);
        UUID orderId = created.orderId();
        assertThat(orderId).isNotNull();
        assertThat(created.customerId()).isEqualTo(customerId);
        assertThat(created.totalAmountCents()).isEqualTo(totalAmountCents);
        assertThat(created.currency()).isEqualTo(currency);

        // Act - fetch by id
        String getByIdResponseBody = mockMvc.perform(get("/orders/{orderId}", orderId)
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - fetch by id response
        GetOrderResponse fetched = objectMapper.readValue(getByIdResponseBody, GetOrderResponse.class);
        assertThat(fetched.orderId()).isEqualTo(orderId);
        assertThat(fetched.customerId()).isEqualTo(customerId);
        assertThat(fetched.items().size()).isEqualTo(1);
        assertThat(fetched.items().get(0).productId()).isEqualTo(productId);
        assertThat(fetched.items().get(0).quantity()).isEqualTo(quantity);
        assertThat(fetched.totalAmountUSDCents()).isEqualTo(totalAmountCents);
        assertThat(fetched.currency()).isEqualTo(currency);
        assertThat(fetched.status()).isEqualTo(OrderStatus.CREATED.toString());
        assertThat(fetched.paymentStatus()).isEqualTo(PaymentStatus.PENDING);

        // Act - fetch by customer
        String getByCustomerResponseBody = mockMvc.perform(get("/orders/customer/{customerId}", customerId)
                .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - fetch by customer response
        List<GetOrderResponse> orders = objectMapper.readValue(
                getByCustomerResponseBody,
                new TypeReference<>() {
                }
        );
        assertThat(orders.size()).isEqualTo(1);
        assertThat(orders.get(0).orderId()).isEqualTo(orderId);
    }


}