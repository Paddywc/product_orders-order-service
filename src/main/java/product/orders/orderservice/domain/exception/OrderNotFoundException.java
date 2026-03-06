package product.orders.orderservice.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when an order is not found
 */
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID orderId) {
        super(String.format("Order with id %s not found", orderId));
    }
}
