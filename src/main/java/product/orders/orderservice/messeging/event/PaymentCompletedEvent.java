package product.orders.orderservice.messeging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when payment has been successfully confirmed
 * <p>
 * This event indicates that the payment Service has completed its saga step successfully.
 */
public record PaymentCompletedEvent(UUID eventId, UUID orderId, Long amountInCents, String currency, Instant occurredAt
) {


}
