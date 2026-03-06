package product.orders.orderservice.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderTest {

    /**
     * Helper method to create a Money object with specified amount and currency.
     *
     * @param amountCenta The amount in cents.
     * @param currency    The currency code.
     * @return A Money object with the specified properties.
     */
    private Money createMoney(long amountCenta, String currency) {
        return new Money(amountCenta, currency);
    }

    private CustomerDetails createCustomerDetails(UUID customerId){
        return new CustomerDetails(customerId, "testemail@gmail.com", "123 Fake Street, Springfield, USA");
    }


    /**
     * Factory method to create an Order instance with mock OrderItem and total amount.
     *
     * @return A new Order instance with mock OrderItem and total amount.
     */
    private Order createOrder() {
        OrderItem item = mock(OrderItem.class);
        when(item.totalPrice()).thenReturn(createMoney(1000, "USD"));
        when(item.getProductId()).thenReturn(UUID.randomUUID());
        when(item.getQuantity()).thenReturn(1);


        return Order.create(createCustomerDetails(UUID.randomUUID()), List.of(item), createMoney(1000, "USD"));
    }


    /**
     * Test case for successful creation of an Order.
     * Ensures the Order instance is created with the expected properties when inputs are valid.
     */
    @Test
    void testCreateOrder_WithValidInputs_CreatesOrderSuccessfully() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Money totalAmount = createMoney(3000, "USD");
        OrderItem item1 = mock(OrderItem.class);
        OrderItem item2 = mock(OrderItem.class);

        when(item1.totalPrice()).thenReturn(createMoney(1000, "USD"));
        when(item2.totalPrice()).thenReturn(createMoney(2000, "USD"));

        List<OrderItem> items = List.of(item1, item2);

        // Act
        Order order = Order.create(createCustomerDetails(customerId), items, totalAmount);

        // Assert
        assertEquals(customerId, order.getCustomerDetails().getCustomerId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(totalAmount, order.getTotalAmount());
        assertEquals(items, order.getItems());
    }

    /**
     * Test case for creation of an Order with an empty item list.
     * Ensures an IllegalArgumentException is thrown when no items are provided.
     */
    @Test
    void createOrder_ConstructedWithNoItems_throwsIllegalArgumentException() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Money totalAmount = createMoney(0, "USD");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Order.create(createCustomerDetails(customerId), List.of(), totalAmount));
    }

    /**
     * Test case for mismatched total amount during order creation.
     * Ensures an IllegalStateException is thrown when the provided total amount does not match the calculated total.
     */
    @Test
    void createOrder_ConstructedWithMismatchedTotalAmount_throwsIllegalStateException() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Money providedTotalAmount = createMoney(5000, "USD");
        OrderItem item1 = mock(OrderItem.class);
        OrderItem item2 = mock(OrderItem.class);

        when(item1.totalPrice()).thenReturn(createMoney(1000, "USD"));
        when(item2.totalPrice()).thenReturn(createMoney(2000, "USD"));

        List<OrderItem> items = List.of(item1, item2);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> Order.create(createCustomerDetails(customerId), items, providedTotalAmount));
    }

    /**
     * Test case for a Null customer ID.
     * Ensures a NullPointerException is thrown when the customerId is null.
     */
    @Test
    void testCreateOrder_ConstructedWithNullCustomer_throwsNullPointerException() {
        // Arrange
        Money totalAmount = createMoney(3000, "USD");
        OrderItem item = mock(OrderItem.class);

        when(item.totalPrice()).thenReturn(createMoney(3000, "USD"));

        List<OrderItem> items = List.of(item);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> Order.create(null, items, totalAmount));
    }

    @Test
    void testMarkInventoryReserved_PaymentNotComplete_RemainsInCreatedState() {
        // Arrange
        Order order = createOrder();

        // Act
        order.markInventoryReserved();

        // Assert
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void testMarkInventoryReserved_PaymentComplete_MarkedConfirmed() {
        // Arrange
        Order order = createOrder();
        order.markPaymentComplete();

        // Act
        order.markInventoryReserved();

        // Assert
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void testMarkInventoryReserved_OrderNotInCreatedState_ThrowsException() {
        // Arrange
        Order order = createOrder();
        order.markPaymentComplete();
        order.markInventoryReserved();

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, order::markInventoryReserved);
        assertTrue(exception.getMessage().contains("in state CONFIRMED"));
    }


    @Test
    void testMarkPaymentComplete_InventoryNotReserved_RemainsInCreatedState() {
        // Arrange
        Order order = createOrder();

        // Act
        order.markPaymentComplete();

        // Assert
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void testMarkPaymentComplete_InventoryReserved_MarkedConfirmed() {
        // Arrange
        Order order = createOrder();
        order.markInventoryReserved();

        // Act
        order.markPaymentComplete();

        // Assert
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void testMarkPaymentComplete_OrderNotInCreatedState_ThrowsException() {
        // Arrange
        Order order = createOrder();
        order.markInventoryReserved();
        order.markPaymentComplete();

        // Act & Assert
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, order::markPaymentComplete);
        assertTrue(exception.getMessage().contains("in state"));
        assertTrue(exception.getMessage().contains("CONFIRMED"));
    }

    @Test
    void testMarkPaymentFailed_OrderInCreated_OrderCancelledAndPaymentMarkedAsFailed() {
        // Arrange
        Order order = createOrder();

        // Act
        order.markPaymentFailedAndCancel();

        // Assert
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }

    @Test
    void testMarkInventoryFailed_OrderInCreate_OrderCancelledAndInventoryReservationMarkedAsFailed(){
        // Arrange
        Order order = createOrder();

        // Act
        order.markInventoryFailedAndCancel();

        // Assert
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(InventoryStatus.FAILED, order.getInventoryStatus());
    }

    @Test
    void testCancelOrder_CancelCompletedOrder_ThrowsException() {
        // Arrange
        Order order = createOrder();
        order.markPaymentComplete();
        order.markInventoryReserved();

        // Act & Assert
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, order::markInventoryFailedAndCancel);
        assertTrue(exception.getMessage().contains("Confirmed order"));
        assertTrue(exception.getMessage().contains("cannot be cancelled"));
    }


}