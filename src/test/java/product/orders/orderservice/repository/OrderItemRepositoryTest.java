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

class OrderItemRepositoryTest extends RepositoryTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void testFindByOrderId_OrderHasItems_ReturnsAllItems() {
        // Arrange
        Money unitPrice = new Money(100L, "USD");
        OrderItem orderItemOne = new OrderItem(UUID.randomUUID(), "Item One", 2, unitPrice);
        OrderItem orderItemTwo = new OrderItem(UUID.randomUUID(), "Item Two", 1, unitPrice);
        OrderItem orderItemThree = new OrderItem(UUID.randomUUID(), "Item Three", 3, unitPrice);

        Order order = Order.create(
                new CustomerDetails(UUID.randomUUID(), "email@example.com", "123"),
                List.of(orderItemOne, orderItemTwo, orderItemThree),
                new Money(600L, "USD")
        );
        order = orderRepository.save(order);

        // Act
        List<OrderItem> returnedItems = orderItemRepository.findByOrderOrderId(order.getOrderId());

        // Assert
        assertThat(returnedItems.size()).isEqualTo(3);
    }
}
