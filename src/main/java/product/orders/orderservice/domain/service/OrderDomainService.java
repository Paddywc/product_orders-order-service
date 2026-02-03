package product.orders.orderservice.domain.service;

import product.orders.orderservice.domain.model.Order;

public interface OrderDomainService {
    /**
     * Checks if the given order is in the CONFIRMED status.
     *
     * @param order The order to check.
     * @return True if the order is confirmed, false otherwise.
     */
    boolean orderIsConfirmed(Order order);
}
