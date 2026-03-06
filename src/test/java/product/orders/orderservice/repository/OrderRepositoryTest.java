package product.orders.orderservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import product.orders.orderservice.domain.model.CustomerDetails;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OrderRepositoryTest extends RepositoryTest {

    @Autowired
    private OrderRepository orderRepository;


    @Test
    void testFindByCustomerId_CustomHasOrders_ReturnsOrdersByCreatedAtDesc() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        CustomerDetails customerDetails = new CustomerDetails(customerId, "email@example.com", "123");
        Money total = new Money(100L, "USD");
        OrderItem orderOneItem = new OrderItem(UUID.randomUUID(), "Test product", 1, total);
        OrderItem orderTwoItem = new OrderItem(UUID.randomUUID(), "Test product", 1, total);

        Order orderOne = Order.create(customerDetails, List.of(orderOneItem), total);
        Order orderTwo = Order.create(customerDetails, List.of(orderTwoItem), total);

        orderOne = orderRepository.saveAndFlush(orderOne);
        orderTwo = orderRepository.saveAndFlush(orderTwo);

        // Act
        List<Order> returnedOrders = orderRepository.findByCustomerDetailsCustomerIdOrderByCreatedAtDescOrderIdDesc(customerId);

        // Assert
        assertThat(returnedOrders.size()).isEqualTo(2);
        assertThat(returnedOrders.get(0).getOrderId()).isEqualTo(orderTwo.getOrderId());
        assertThat(returnedOrders.get(1).getOrderId()).isEqualTo(orderOne.getOrderId());
    }
}
