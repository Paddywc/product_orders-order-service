package product.orders.orderservice.messeging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when inventory reservation fails
 * <p>
 * This event signals a saga failure and should trigger
 * order cancellation and compensation.
 */
public record InventoryReservationFailedEvent(
        UUID eventId,
        UUID orderId,
        InventoryReservationFailedReason reason,
        String failureMessage,
        Instant occurredAt
) {


}
