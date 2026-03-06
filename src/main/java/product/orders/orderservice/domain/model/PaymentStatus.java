package product.orders.orderservice.domain.model;

/**
 * Status of the user's payment for an {@link Order}. Must be completed for the order to be confirmed
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}
