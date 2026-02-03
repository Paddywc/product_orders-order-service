package product.orders.orderservice.messeging.event;

import product.orders.orderservice.validation.currency.ValidCurrency;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emitted when an order is successfully created and
 * the order saga should begin.
 */
public record OrderCreatedEvent(
        /**
         * Unique event id. Prevent duplication
         */
        UUID eventId,

        /**
         * Unique id of the order that was created
         */
        UUID orderId,

        /**
         * Total amount of the order in cents
         */
        long totalAmountCents,

        /**
         * The currency of the total amount in cents
         */
        @ValidCurrency String currency,

        /**
         * Time order occurred
         */
        Instant occurredAt,
        /**
         * Items that were created
         */
        List<OrderItem> items) {

    public OrderCreatedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public static OrderCreatedEvent of(UUID orderId, Long totalAmountCents, String currency, List<OrderItem> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),   // event identity
                orderId,
                totalAmountCents,
                currency,
                Instant.now(),
                List.copyOf(items)   // defensive copy
        );
    }
}