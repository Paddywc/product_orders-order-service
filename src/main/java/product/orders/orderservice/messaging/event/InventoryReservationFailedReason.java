package product.orders.orderservice.messaging.event;

/**
 * Reasons for failing to reserve inventory in an {@link InventoryReservationFailedEvent}
 */
public enum InventoryReservationFailedReason {
   INSUFFICIENT_INVENTORY,
   SERVER_DATA_ERROR,
   INVALID_REQUEST,
   DUPLICATE_RESERVATION,
   ILLEGAL_RESERVATION_STATE
}
