package product.orders.orderservice.messeging.event;

public enum CancellationReason {
    INVENTORY_RESERVATOIN_FAILED,
    PAYMENT_FAILED,
    USER_CANCELLED,
    TIMEOUT,
    SYSTEM_ERROR
}
