package product.orders.orderservice.persistance;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * An event from the inventory service that has been processed
 */
@Entity
@Table(name = "processed_inventory_event")
public class ProcessedInventoryEvent extends ProcessedEvent {

    public ProcessedInventoryEvent() {
    }

    public ProcessedInventoryEvent(UUID eventId) {
        super(eventId);
    }
}
