package product.orders.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.orderservice.persistance.ProcessedInventoryEvent;

import java.util.UUID;

public interface ProcessedInventoryEventRepository extends JpaRepository<ProcessedInventoryEvent, Long> {
    boolean existsByEventId(UUID eventId);
}
