package product.orders.orderservice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import product.orders.orderservice.api.dto.CreateOrderItemRequest;
import product.orders.orderservice.api.dto.CreateOrderRequest;
import product.orders.orderservice.application.OrderApplicationService;
import product.orders.orderservice.domain.model.CustomerDetails;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class,
        // Remove Oauth2 security
        excludeAutoConfiguration = {
                org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet
                        .OAuth2ResourceServerAutoConfiguration.class
        }
)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderApplicationService orderApplicationService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void testCreateOrder_WhenValidRequest_ReturnsCreatedResponse() throws Exception {
        // Arrange
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(UUID.randomUUID(), "Test product", 2, 500L)
                ),
                customerId,
                "fakeemail@gmail.com",
                "123 Fake Stress",
                1000L,
                "USD"
        );

        UUID orderId = UUID.randomUUID();
        when(orderApplicationService.createOrder(any(CustomerDetails.class), any(), any())).thenReturn(orderId);

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.totalAmountCents").value(request.totalAmountCents()))
                .andExpect(jsonPath("$.currency").value(request.currency()));
    }


    @Test
    void testCreateOrder_WhenValidRequest_CreatesOrder() throws Exception {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        int productQuantity = 2;
        long productPrice = 500L;
        long totalAmount = 1000L;
        String currency = "USD";
        String email = "fakeemail@gmail.com";
        String customerAddress = "21 Jump Street";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(productId, "Test product", productQuantity, productPrice)
                ),
                customerId,
                email,
                customerAddress,
                totalAmount,
                currency
        );

        UUID orderId = UUID.randomUUID();
        when(orderApplicationService.createOrder(any(), any(), any())).thenReturn(orderId);

        // Capture arguments sent to application service
        ArgumentCaptor<CustomerDetails> customerDetailsCapture = ArgumentCaptor.forClass(CustomerDetails.class);
        ArgumentCaptor<List<OrderItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);

        // Act
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));


        // Assert
        verify(orderApplicationService).createOrder(
                customerDetailsCapture.capture(),
                itemsCaptor.capture(),
                moneyCaptor.capture()
        );

        List<OrderItem> capturedItems = itemsCaptor.getValue();
        Money capturedMoney = moneyCaptor.getValue();

        assertThat(capturedItems).hasSize(1);

        OrderItem item = capturedItems.get(0);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getQuantity()).isEqualTo(productQuantity);
        assertThat(item.getUnitPrice().getAmountCents()).isEqualTo(productPrice);
        assertThat(item.getUnitPrice().getCurrency()).isEqualTo(currency);

        assertThat(customerDetailsCapture.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(customerDetailsCapture.getValue().getCustomerEmail()).isEqualTo(email);
        assertThat(customerDetailsCapture.getValue().getCustomerAddress()).isEqualTo(customerAddress);


        assertThat(capturedMoney.getAmountCents()).isEqualTo(totalAmount);
        assertThat(capturedMoney.getCurrency()).isEqualTo(currency);
    }

    @Test
    void testCreateOrder_WhenMissingCustomerId_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(UUID.randomUUID(), "Test product", 2, 500L)
                ),
                null,
                "fakeemail@gmail.com",
                "A street, Somewhere, USA",
                0L,
                "USD"
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_WhenInvalidCurrency_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(UUID.randomUUID(), "Test product", 2, 500L)
                ),
                UUID.randomUUID(),
                "fakeemail@gmail.com",
                "123 fake street",
                1000L,
                "INVALID_CURRENCY"
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_WhenOrderItemQuantityIsNegative_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(UUID.randomUUID(), "Test product", -1, 500L)
                ),
                UUID.randomUUID(),
                "fakeemail@gmail.com",
                "123 Fake Street, Springfield, USA",
                1000L,
                "USD"
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void testCreateOrder_WhenNonEmailGiven_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new CreateOrderItemRequest(UUID.randomUUID(), "Test product", 2, 500L)
                ),
                UUID.randomUUID(),
                "Not email address",
                "123 Fake Street, Springfield, USA",
                1000L,
                "USD"
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void testCreateOrder_WhenEmptyOrderItems_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(),
                UUID.randomUUID(),
                "fakeemail@gmail.com",
                "123 Fake Street, Springfield, USA",
                1000L,
                "USD"
        );

        // Act & Assert
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrder_WhenOrderExists_ReturnsOrderResponse() throws Exception {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Order order = buildOrder(customerId);
        when(orderApplicationService.getOrder(order.getOrderId())).thenReturn(order);

        // Act & Assert
        mockMvc.perform(get("/orders/{orderId}", order.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getOrderId().toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.customerEmail").value(order.getCustomerDetails().getCustomerEmail()))
                .andExpect(jsonPath("$.customerAddress").value(order.getCustomerDetails().getCustomerAddress()))
                .andExpect(jsonPath("$.totalAmountUSDCents").value(order.getTotalAmount().getAmountCents()))
                .andExpect(jsonPath("$.currency").value(order.getTotalAmount().getCurrency()))
                .andExpect(jsonPath("$.status").value(order.getStatus().toString()))
                .andExpect(jsonPath("$.items[0].productId").value(order.getItems().get(0).getProductId().toString()))
                .andExpect(jsonPath("$.items[0].productName").value(order.getItems().get(0).getProductName()))
                .andExpect(jsonPath("$.items[0].quantity").value(order.getItems().get(0).getQuantity()))
                .andExpect(jsonPath("$.items[0].unitPriceUSDCents").value(order.getItems().get(0).getUnitPrice().getAmountCents()));
    }

    @Test
    void testGetCustomerOrders_WhenOrdersExist_ReturnsOrderList() throws Exception {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Order firstOrder = buildOrder(customerId);
        Order secondOrder = buildOrder(customerId);
        when(orderApplicationService.getCustomerOrders(customerId)).thenReturn(List.of(firstOrder, secondOrder));

        // Act & Assert
        mockMvc.perform(get("/orders/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(firstOrder.getOrderId().toString()))
                .andExpect(jsonPath("$[1].orderId").value(secondOrder.getOrderId().toString()))
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$[1].customerId").value(customerId.toString()));
    }

    private static Order buildOrder(UUID customerId) {
        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                "Widget",
                2,
                new Money(500L, "USD")
        );
        CustomerDetails customerDetails = new CustomerDetails(
                customerId,
                "customer@example.com",
                "123 Example Street"
        );
        return Order.create(customerDetails, List.of(item), new Money(1000L, "USD"));
    }
}
