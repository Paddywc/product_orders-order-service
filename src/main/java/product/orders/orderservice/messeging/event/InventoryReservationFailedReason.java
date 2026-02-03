package product.orders.orderservice.messeging.event;

public enum InventoryReservationFailedReason {
   INSUFFICIENT_INVENTORY,
   INVALID_REQUEST,
   DUPLICATE_RESERVATION,
   ILLEGAL_RESERVATION_STATE
}
