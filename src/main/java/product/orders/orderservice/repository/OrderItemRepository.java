package product.orders.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.orderservice.domain.model.OrderItem;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
