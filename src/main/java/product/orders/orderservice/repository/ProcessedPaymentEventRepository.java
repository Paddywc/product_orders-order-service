package product.orders.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.orderservice.persistance.ProcessedPaymentEvent;

import java.util.UUID;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, Long> {
    boolean existsByEventId(UUID eventId);

}
