package product.orders.orderservice.application.saga;

import org.springframework.transaction.annotation.Transactional;
import product.orders.orderservice.application.OrderApplicationService;
import product.orders.orderservice.messeging.event.*;
import product.orders.orderservice.messeging.producer.OrderEventProducer;

public class OrderSagaHandler {

    private final OrderApplicationService orderApplicationService;


    private final OrderEventProducer orderEventProducer;

    public OrderSagaHandler(OrderApplicationService orderApplicationService,OrderEventProducer orderEventProducer) {
        this.orderApplicationService = orderApplicationService;
        this.orderEventProducer = orderEventProducer;
    }


    /**
     * Mark the inventory reserved for the order and publish the order confirmed event if the order is now confirmed
     * @param event information about the inventory reservation
     */
    @Transactional(value = "transactionManager")
    public void markInventoryReserved(InventoryReservedEvent event){
        boolean orderConfirmed = orderApplicationService.markOrderInventoryReserved(event.orderId());
        if(orderConfirmed){
            orderEventProducer.publish(OrderConfirmedEvent.of(event.orderId()));
        }
    }

    /**
     * Mark the inventory reservation as failed for the order, cancel the order, and publish the order canceled event
     * @param event information about the reservation failure
     */
    public void markInventoryReservationFailed(InventoryReservationFailedEvent event){
        orderApplicationService.cancelOrder(event.orderId());
        orderEventProducer.publish(OrderCancelledEvent.of(event.orderId(), CancellationReason.INVENTORY_RESERVATOIN_FAILED));
    }

    /**
     * Mark the payment complete for the order and publish the order confirmed event if the order is now confirmed
     * @param event information about the payment completion
     */
    @Transactional(value = "transactionManager")
    public void markPaymentComplete(PaymentCompletedEvent event){
        boolean orderConfirmed = orderApplicationService.markOrderPaymentComplete(event.orderId());
        if(orderConfirmed){
            orderEventProducer.publish(OrderConfirmedEvent.of(event.orderId()));
        }
    }

    /**
     * Mark the payment failed for the order, cancel the order, and publish the order canceled event
     * @param event information about the payment failure
     */
    @Transactional(value = "transactionManager")
    public void markPaymentFailed(PaymentFailedEvent event){
        orderApplicationService.cancelOrder(event.orderId());
        orderEventProducer.publish(OrderCancelledEvent.of(event.orderId(), CancellationReason.PAYMENT_FAILED));
    }
}
