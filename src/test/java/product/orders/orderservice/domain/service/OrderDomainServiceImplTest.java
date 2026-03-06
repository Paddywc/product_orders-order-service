package product.orders.orderservice.domain.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.repository.OrderRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link OrderDomainServiceImpl} class.
 * Verifies the behavior of the {@code orderIsConfirmed} method.
 */
class OrderDomainServiceImplTest {

    /**
     * Test when the order status is CONFIRMED, the method returns true.
     */
    @Test
    void testOrderIsConfirmed_OrderStatusConfirmed_ReturnsTrue() {
        // Arrange
        Order mockOrder = mock(Order.class);
        when(mockOrder.getStatus()).thenReturn(OrderStatus.CONFIRMED);
        OrderDomainServiceImpl service = new OrderDomainServiceImpl();

        // Act
        boolean result = service.orderIsConfirmed(mockOrder);

        // Assert
        assertTrue(result, "Expected orderIsConfirmed to return true for CONFIRMED status.");
    }

    /**
     * Test when the order status is not CONFIRMED, the method returns false.
     */
    @Test
    void testOrderIsConfirmed_OrderStatusNotConfirmed_ReturnsFalse() {
        // Arrange
        Order mockOrder = mock(Order.class);
        when(mockOrder.getStatus()).thenReturn(OrderStatus.CREATED);
        OrderDomainServiceImpl service = new OrderDomainServiceImpl();

        // Act
        boolean result = service.orderIsConfirmed(mockOrder);

        // Assert
        assertFalse(result, "Expected orderIsConfirmed to return false for non-CONFIRMED status.");
    }
}