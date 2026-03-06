package product.orders.orderservice.domain.model;

/**
 * Status of the inveontory fullfillment for the items in an order {@link Order}. Must be reserved for an order to be
 * completed
 */
public enum InventoryStatus {
    PENDING,
    RESERVED,
    FAILED,
    RELEASED
}
