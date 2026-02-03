package product.orders.orderservice.application;

import org.springframework.transaction.annotation.Transactional;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;

public interface OrderApplicationService {
    /**
     * Create the order, save it to be the DB, and fire an order created event
     *
     * @param customerId  the id of the consumer who made the order
     * @param items       the items in the order
     * @param totalAmount the total amount of the order
     * @return the order id
     */
    @Transactional
    UUID createOrder(UUID customerId, List<OrderItem> items, Money totalAmount);

    /**
     * Mark the parameter order has having its inventory complete and save it
     * @param orderId the id of the order to mark the inventory complete for
     * @return true if the order is now confirmed, false otherwise
     */
    boolean markOrderInventoryReserved(UUID orderId);

    /**
     * Mark the parameter order has having its payment complete and save it
     * @param orderId the id of the order to mark the payment complete for
     * @return true if the order is now confirmed, false otherwise
     */
    boolean markOrderPaymentComplete(UUID orderId);

    /**
     * Cancel the order with the given id
     * @param orderId the id of the order to cancel
     */
    void cancelOrder(UUID orderId);
}
