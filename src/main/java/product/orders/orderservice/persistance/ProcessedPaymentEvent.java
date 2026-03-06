package product.orders.orderservice.persistance;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * An event from the payment service that has been processed
 */
@Entity
@Table(name = "processed_payment_event")
public class ProcessedPaymentEvent extends ProcessedEvent {

    public ProcessedPaymentEvent() {
    }

    public ProcessedPaymentEvent(UUID eventId) {
        super(eventId);
    }
}
