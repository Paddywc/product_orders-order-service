package product.orders.orderservice.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.orders.orderservice.domain.exception.OrderNotFoundException;
import product.orders.orderservice.domain.model.CustomerDetails;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.domain.service.OrderDomainService;
import product.orders.orderservice.messaging.event.InventoryReleasedEvent;
import product.orders.orderservice.messaging.event.OrderCreatedEvent;
import product.orders.orderservice.messaging.event.OrderItem;
import product.orders.orderservice.messaging.producer.OrderEventProducer;
import product.orders.orderservice.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationServiceImpl implements OrderApplicationService {

    private final OrderRepository orderRepository;

    private final OrderEventProducer orderEventProducer;

    private final OrderDomainService orderDomainService;

    public OrderApplicationServiceImpl(OrderRepository orderRepository, OrderEventProducer orderEventProducer, OrderDomainService orderDomainService) {
        this.orderRepository = orderRepository;
        this.orderEventProducer = orderEventProducer;
        this.orderDomainService = orderDomainService;
    }

    @Transactional(value = "transactionManager")
    @Override
    public UUID createOrder(CustomerDetails customerDetails, List<product.orders.orderservice.domain.model.OrderItem> items, Money totalAmount) {
        Order order = Order.create(customerDetails, items, totalAmount);
        orderRepository.save(order);


        // Pass as event order items
        List<OrderItem> eventItems = items.stream()
                .map(item -> new OrderItem(item.getProductId(), item.getQuantity()))
                .toList();

        orderEventProducer.publish(
                OrderCreatedEvent.of(
                        order.getOrderId(),
                        totalAmount.getAmountCents(),
                        totalAmount.getCurrency(),
                        customerDetails.getCustomerId(),
                        customerDetails.getCustomerEmail(),
                        customerDetails.getCustomerAddress(),
                        eventItems
                )
        );

        return order.getOrderId();
    }

    /**
     * Mark the parameter order has having its inventory complete and save it
     *
     * @param orderId the id of the order to mark the inventory complete for
     * @return true if the order is now confirmed, false otherwise
     */
    @Override
    @Transactional(value = "transactionManager")
    public boolean markOrderInventoryReserved(UUID orderId) {
        Order order = getOrder(orderId);
        order.markInventoryReserved();
        orderRepository.save(order);

        return orderDomainService.orderIsConfirmed(order);
    }


    /**
     * Mark the parameter order has having its payment complete and save it
     *
     * @param orderId the id of the order to mark the payment complete for
     * @return true if the order is now confirmed, false otherwise
     */
    @Override
    @Transactional(value = "transactionManager")
    public boolean markOrderPaymentComplete(UUID orderId) {
        Order order = getOrder(orderId);
        order.markPaymentComplete();
        orderRepository.save(order);

        return orderDomainService.orderIsConfirmed(order);

    }

    /**
     * Mark the inventory reservation for the order with the given id as failed and cancel the order
     *
     * @param orderId the id of the order to mark the inventory reservation as failed for
     */
    @Transactional(value = "transactionManager")
    @Override
    public void markInventoryReservationFailedAndCancel(UUID orderId) {
        Order order = getOrder(orderId);
        order.markInventoryFailedAndCancel();
        orderRepository.save(order);
    }


    /**
     * Mark the payment as failed for the order. Cancel the order if it is not already confirmed.
     */
    @Transactional(value = "transactionManager")
    @Override
    public void markPaymentFailedAndCancel(UUID orderId) {
        Order order = getOrder(orderId);
        order.markPaymentFailedAndCancel();
        orderRepository.save(order);
    }

    /**
     * Get the status of the order with the given id
     *
     * @param orderId the id of the order to get the status for
     * @return the status of the order
     */
    @Override
    public OrderStatus getOrderStatus(UUID orderId) {
        return this.getOrder(orderId).getStatus();
    }

    /**
     * Mark the inventory status as released for the order with the given id
     *
     * @param event the event containing the order id
     */
    @Override
    public void markInventoryReleased(InventoryReleasedEvent event) {
        Order order = this.getOrder(event.orderId());
        order.markInventoryReleased();
        orderRepository.save(order);
    }

    /**
     * Mark the payment as refunded for the order with the given id
     *
     * @param orderId the id of the order to mark the payment as refunded for
     */
    @Override
    public void markPaymentRefunded(UUID orderId) {
        Order order = this.getOrder(orderId);
        order.markPaymentRefunded();
        orderRepository.save(order);
    }


    /**
     * Get the order with the given id
     *
     * @param orderId the id of the order to get
     */
    @Override
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Get all orders for the given customer
     *
     * @param customerId the id of the customer to get orders for
     * @return a list of orders for the customer
     */
    @Override
    public List<Order> getCustomerOrders(UUID customerId) {
        return orderRepository.findByCustomerDetailsCustomerIdOrderByCreatedAtDescOrderIdDesc(customerId);
    }


}
