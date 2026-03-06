package product.orders.orderservice.application;

import org.springframework.transaction.annotation.Transactional;
import product.orders.orderservice.messaging.event.InventoryReleasedEvent;
import product.orders.orderservice.domain.model.*;

import java.util.List;
import java.util.UUID;

public interface OrderApplicationService {
    /**
     * Create the order, save it to be the DB, and fire an order created event
     *
     * @param customerDetails information about the customer who made the order
     * @param items       the items in the order
     * @param totalAmount the total amount of the order
     * @return the order id
     */
    @Transactional
    UUID createOrder(CustomerDetails customerDetails, List<OrderItem> items, Money totalAmount);

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
     * Mark the inventory reservation for the order with the given id as failed and cancel the order
     * @param orderId the id of the order to mark the inventory reservation as failed for
     */
    @Transactional(value = "transactionManager")
    void markInventoryReservationFailedAndCancel(UUID orderId);

    /**
     * Mark the payment as failed for the order. Cancel the order if it is not already confirmed.
     * @param orderId the id of the order to mark the payment as failed for
     */
    @Transactional(value = "transactionManager")
    void markPaymentFailedAndCancel(UUID orderId);

    /**
     * Get the status of the order with the given id
     * @param orderId the id of the order to get the status for
     * @return the status of the order
     */
    OrderStatus getOrderStatus(UUID orderId);

    /**
     * Mark the inventory status as released for the order with the given id
     * @param event the event containing the order id
     */
    void markInventoryReleased(InventoryReleasedEvent event);

    /**
     * Mark the payment as refunded for the order with the given id
     * @param orderId the id of the order to mark the payment as refunded for
     */
    void markPaymentRefunded(UUID orderId);

    /**
     * Get the order with the given id
     * @param orderId the id of the order to get
     */
    Order getOrder(UUID orderId);

    /**
     * Get all orders for the given customer
     * @param customerId the id of the customer to get orders for
     * @return a list of orders for the customer
     */
    List<Order> getCustomerOrders(UUID customerId);
}
