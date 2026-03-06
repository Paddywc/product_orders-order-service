package product.orders.orderservice.persistance;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An order event with a unique id that has been processed
 */
@MappedSuperclass
public abstract class ProcessedEvent {

    /**
     * Use unique id rather than the eventId because otherwise it will try to merge when an order id is set, but
     * we want to throw an exception if the event has already been processed
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, name = "event_id", unique = true)
    private UUID eventId;

    @Column(nullable = false, updatable = false, name = "processed_at")
    private Instant processedAt;

    protected ProcessedEvent() {
        // JPA only
    }

    protected ProcessedEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}