package product.orders.orderservice.messeging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a payment has failed.
 * <p>
 * This event is terminal for the payment saga and is used
 * by downstream services to perform compensation actions.
 */
public record PaymentFailedEvent(UUID eventId, UUID orderId, String reason, Instant occurredAt) {


}
