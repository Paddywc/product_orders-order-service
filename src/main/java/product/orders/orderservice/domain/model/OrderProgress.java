package product.orders.orderservice.domain.model;

/**
 * Summary of the progress of an order. Summarizes the {@link OrderStatus},  {@link InventoryStatus} and
 * the {@link PaymentStatus} of an order as a single enum
 */
public enum OrderProgress {
    AWAITING_PAYMENT_AND_INVENTORY_RESERVATION,
    AWAITING_INVENTORY_RESERVATION,
    AWAITING_PAYMENT,
    CONFIRMED,
    CANCELLED_AWAITING_PAYMENT_REFUND,
    CANCELLED_INVENTORY_RESERVATION_FAILED,
    CANCELLED_PAYMENT_FAILED
}
