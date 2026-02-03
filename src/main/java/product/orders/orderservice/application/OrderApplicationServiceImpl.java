package product.orders.orderservice.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderItem;
import product.orders.orderservice.domain.service.OrderDomainService;
import product.orders.orderservice.messeging.event.OrderCreatedEvent;
import product.orders.orderservice.messeging.producer.OrderEventProducer;
import product.orders.orderservice.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

@Service
public class OrderApplicationServiceImpl implements OrderApplicationService {

    private OrderRepository orderRepository;

    private OrderEventProducer orderEventProducer;

    private OrderDomainService orderDomainService;

    @Transactional
    @Override
    public UUID createOrder(UUID customerId, List<OrderItem> items, Money totalAmount){
        Order order = Order.create(customerId, items, totalAmount);
        orderRepository.save(order);


        // Pass as event order items
        List<product.orders.orderservice.messeging.event.OrderItem> eventItems = items.stream()
                .map(item -> new product.orders.orderservice.messeging.event.OrderItem(item.getProductId(), item.getQuantity()))
                .toList();

        orderEventProducer.publish(
                OrderCreatedEvent.of(
                        order.getOrderId(),
                        totalAmount.getAmountCents(),
                        totalAmount.getCurrency(),
                        eventItems
                )
        );

        return order.getOrderId();
    }

    /**
     * Mark the parameter order has having its inventory complete and save it
     * @param orderId the id of the order to mark the inventory complete for
     * @return true if the order is now confirmed, false otherwise
     */
    @Override
    public boolean markOrderInventoryReserved(UUID orderId){
        Order order = orderRepository.getReferenceById(orderId);
        order.markInventoryReserved();
        orderRepository.save(order);

        return orderDomainService.orderIsConfirmed(order);
    }


    /**
     * Mark the parameter order has having its payment complete and save it
     * @param orderId the id of the order to mark the payment complete for
     * @return true if the order is now confirmed, false otherwise
     */
    @Override
    public boolean markOrderPaymentComplete(UUID orderId){
        Order order = orderRepository.getReferenceById(orderId);
        order.markInventoryReserved();
        orderRepository.save(order);

        return orderDomainService.orderIsConfirmed(order);

    }

    /**
     * Cancel the order with the given id
     * @param orderId the id of the order to cancel
     */
    @Override
    public void cancelOrder(UUID orderId){
        Order order = orderRepository.getReferenceById(orderId);
        order.cancel();
        orderRepository.save(order);
    }





}
