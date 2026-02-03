package product.orders.orderservice.messeging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when inventory has been successfully reserved
 * for all items in an order.
 * <p>
 * This event indicates that the Inventory Service has
 * completed its saga step successfully.
 */
public record InventoryReservedEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt
) {


}
