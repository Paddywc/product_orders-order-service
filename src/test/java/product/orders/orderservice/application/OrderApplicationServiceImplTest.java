package product.orders.orderservice.application;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import product.orders.orderservice.domain.exception.OrderNotFoundException;
import product.orders.orderservice.messaging.event.OrderCreatedEvent;
import product.orders.orderservice.messaging.event.InventoryReleasedEvent;
import product.orders.orderservice.domain.model.CustomerDetails;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderItem;
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.domain.service.OrderDomainService;
import product.orders.orderservice.messaging.producer.OrderEventProducer;
import product.orders.orderservice.repository.OrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceImplTest {

    @InjectMocks
    private OrderApplicationServiceImpl orderApplicationService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private OrderDomainService orderDomainService;

    private CustomerDetails createMockCustomerDetails(UUID customerId){
        CustomerDetails customerDetails = mock(CustomerDetails.class);

        return customerDetails;
    }


    @Test
    void testCreateOrder_GivenValidOrder_ShouldPublishOrderCreatedEventWithSentData() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Long totalAmountPaid = 100L;
        String currency = "USD";
        int quantity = 1;
        Money totalAmount = new Money(totalAmountPaid, currency);
        List<OrderItem> orderItems = List.of(
                new OrderItem(productId, "test product", quantity, totalAmount)
        );

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);


        // Act
        orderApplicationService.createOrder(createMockCustomerDetails(customerId), orderItems, totalAmount);

        // Assert
        // Get the event passed to orderEventProducer.publish
        verify(orderEventProducer).publish(eventCaptor.capture());
        OrderCreatedEvent publishedEvent = eventCaptor.getValue();

        assertEquals(totalAmountPaid, publishedEvent.totalAmountCents());
        assertEquals(currency, publishedEvent.currency());
        assertEquals(1, publishedEvent.items().size());
        assertEquals(productId, publishedEvent.items().get(0).productId());
        assertEquals(quantity, publishedEvent.items().get(0).quantity());
    }

    @Test
    void testCreateOrder_GivenValidOrder_ReturnsIdOfNewOrder(){
        // Arrange
        UUID customerId = UUID.randomUUID();
        long totalAmountPaid = 200L;
        Money totalAmount = new Money(totalAmountPaid, "EUR");
        List<OrderItem> orderItems = List.of(
                new OrderItem(UUID.randomUUID(),"test", 1, totalAmount)
        );

        ArgumentCaptor<Order> orderCapture = ArgumentCaptor.forClass(Order.class);

        // Act
        UUID returnedId = orderApplicationService.createOrder(createMockCustomerDetails(customerId), orderItems, totalAmount);

        // Assert
        assertNotNull(returnedId);
        verify(orderRepository).save(orderCapture.capture());
        assertEquals(orderCapture.getValue().getOrderId(), returnedId);
    }

    @Test
    void testMarkOrderInventoryReserved_GivenValidOrderId_MarksInventoryReservedAndSaved(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        orderApplicationService.markOrderInventoryReserved(orderId);

        // Assert
        verify(order).markInventoryReserved();
        verify(orderRepository).save(order);
    }


    @Test
    void testMarkOrderInventoryReserved_OrderIsConfirmedReturnsTrue_ReturnsTrue(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDomainService.orderIsConfirmed(order)).thenReturn(true);

        // Act
        boolean result = orderApplicationService.markOrderInventoryReserved(orderId);

        // Assert
        assertTrue(result);
    }


    @Test
    void testMarkOrderInventoryReserved_OrderIsConfirmedReturnsFalse_ReturnsFalse(){
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDomainService.orderIsConfirmed(order)).thenReturn(false);

        // Act
        boolean result = orderApplicationService.markOrderInventoryReserved(orderId);

        // Assert
        assertFalse(result);
    }


    @Test
    void testMarkOrderPaymentComplete_GivenValidOrderId_MarksPaymentCompleteAndSaved() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        orderApplicationService.markOrderPaymentComplete(orderId);

        // Assert
        verify(order).markPaymentComplete();
        verify(orderRepository).save(order);
    }

    @Test
    void testMarkOrderPaymentComplete_OrderIsConfirmedReturnsTrue_ReturnsTrue() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDomainService.orderIsConfirmed(order)).thenReturn(true);

        // Act
        boolean result = orderApplicationService.markOrderPaymentComplete(orderId);

        // Assert
        assertTrue(result);
    }

    @Test
    void testMarkOrderPaymentComplete_OrderIsConfirmedReturnsFalse_ReturnsFalse() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = mock(Order.class);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDomainService.orderIsConfirmed(order)).thenReturn(false);

        // Act
        boolean result = orderApplicationService.markOrderPaymentComplete(orderId);

        // Assert
        assertFalse(result);
    }
}