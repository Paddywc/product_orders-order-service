package product.orders.orderservice.messeging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an order is cancelled.

 * This event is terminal for the order saga and is used
 * by downstream services (e.g. Inventory) to perform
 * compensation actions.
 */

public record OrderCancelledEvent(
        /**
         * Unique event id. Prevent duplication
         */
        UUID eventId,

        /**
         * Unique id of the order that was canceled
         */
        UUID orderId,

        /**
         * The reason why the order was cancelled
         */
        CancellationReason reason,

        /**
         * Time the order was cancelled
         */
        Instant occurredAt) {

    public OrderCancelledEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("cancellation reason must not be null");
        }
    }

    public static OrderCancelledEvent of(UUID orderId, CancellationReason reason){
        return new OrderCancelledEvent(
                UUID.randomUUID(),
                orderId,
                reason,
                Instant.now()
        );
    }

}