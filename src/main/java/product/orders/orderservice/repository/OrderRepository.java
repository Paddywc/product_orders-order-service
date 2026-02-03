package product.orders.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.orderservice.domain.model.Order;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
