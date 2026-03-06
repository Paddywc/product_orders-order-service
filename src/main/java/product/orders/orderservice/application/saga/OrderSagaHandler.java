package product.orders.orderservice.application.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import product.orders.orderservice.application.OrderApplicationService;
import product.orders.orderservice.domain.exception.OrderNotFoundException;
import product.orders.orderservice.domain.model.OrderStatus;
import product.orders.orderservice.messaging.event.*;
import product.orders.orderservice.messaging.producer.OrderEventProducer;

@Service
public class OrderSagaHandler {

    private final OrderApplicationService orderApplicationService;


    private final OrderEventProducer orderEventProducer;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public OrderSagaHandler(OrderApplicationService orderApplicationService, OrderEventProducer orderEventProducer) {
        this.orderApplicationService = orderApplicationService;
        this.orderEventProducer = orderEventProducer;
    }


    /**
     * Mark the inventory reserved for the order and publish the order confirmed event if the order is now confirmed.
     * If the reservation fails, cancel the order and publish the order canceled event.
     *
     * @param event information about the inventory reservation
     */
    public void markInventoryReserved(InventoryReservedEvent event) {
        try {
            boolean orderConfirmed = orderApplicationService.markOrderInventoryReserved(event.orderId());
            logger.info("Inventory reservation marked for order: {}", event.orderId());
            if (orderConfirmed) {
                logger.info("Order confirmed after inventory reservation: {}", event.orderId());
                orderEventProducer.publish(OrderConfirmedEvent.of(event.orderId()));
            }
        } catch (OrderNotFoundException e) {
            logger.debug("Order not found for inventory reservation: {}", event.orderId());
            orderEventProducer.publish(OrderCancelledEvent.of(event.orderId(), CancellationReason.ORDER_NOT_FOUND));
        } catch (IllegalStateException e) { // Ordder is not in the CREATED state, can not have its inventory reserved
            logger.debug(e.getLocalizedMessage());
        }
    }

    /**
     * Mark the inventory reservation as failed for the order, cancel the order, and publish the order canceled event
     * If the cancellation fails, log the error and publish the order canceled event with the failure reason. If the
     * order is already confirmed, log a warning but do nothing
     *
     * @param event information about the reservation failure
     */
    public void markInventoryReservationFailed(InventoryReservationFailedEvent event) {
        try {
            orderApplicationService.markInventoryReservationFailedAndCancel(event.orderId());
            orderEventProducer.publish(OrderCancelledEvent.of(event.orderId(), CancellationReason.INVENTORY_RESERVATION_FAILED));
            logger.info("Inventory reservation failed for order: {}", event.orderId());
        } catch (OrderNotFoundException e) { // No order.  Fire order canceled event to trigger compensating transactions
            logger.debug("Failed to cancel order due to order not being found in the database: {}. Cancelling order with ORDER_NOT_FOUND reason", event.orderId());
            orderEventProducer.publish(OrderCancelledEvent.of(event.orderId(), CancellationReason.ORDER_NOT_FOUND));
        } catch (IllegalStateException e) { // Trying to cancel a confirmed order
            logger.debug(e.getLocalizedMessage());
            // Event can not be canceled if it is already confirmed, do not published canceled event
        }
    }

    /**
     * Mark the payment complete for the order and publish the order confirmed event if the order is now confirmed
     *
     * @param event information about the payment completion
     */
    public void markPaymentComplete(PaymentCompletedEvent event) {
        try {
            boolean orderConfirmed = orderApplicationService.markOrderPaymentComplete(event.orderId());
            logger.info("Payment confirmation marked for order: {}", event.orderId());

            if (orderConfirmed) {
                logger.info("Order confirmed after payment confirmation: {}", event.orderId());
                orderEventProducer.publish(OrderConfirmedEvent.of(event.orderId()));
            }
        } catch (OrderNotFoundException e) {
            logger.debug("Order not found for payment confirmation: {}", event.orderId());
            orderEventProducer.publish(OrderCancelledEvent.of(event.orderId(), CancellationReason.ORDER_NOT_FOUND));
        } catch (IllegalStateException e) { // Order is either already confirmed or canceled
            logger.debug(e.getLocalizedMessage());
            // If the order is canceled, the payment will have to be refunded
            if(orderApplicationService.getOrderStatus(event.orderId()) == OrderStatus.CANCELLED){
                logger.info("Order is already canceled, sending invalid payment event to refund payment");
                orderEventProducer.publish(InvalidPaymentMadeEvent.of(event.orderId(), event.paymentId()));
            }

        }
    }

    /**
     * Mark the payment failed for the order, cancel the order, and publish the order canceled event
     *
     * @param event information about the payment failure
     */
    public void markPaymentFailed(PaymentFailedEvent event) {
        try {
            orderApplicationService.markPaymentFailedAndCancel(event.orderId());
            orderEventProducer.publish(OrderCancelledEvent.of(event.orderId(), CancellationReason.PAYMENT_FAILED));
        }catch (IllegalStateException e){
            // Order is already confirmed and can not be canceled
            logger.debug(e.getLocalizedMessage());
        }
    }

    /**
     * Mark the order as having its inventory released.
     * @param event the event containing the order id
     */
    public void markInventoryReleased(InventoryReleasedEvent event){
        orderApplicationService.markInventoryReleased(event);
    }

    /**
     * Mark the order as having its payment refunded.
     * @param event the event containing the order id
     */
    public void markPaymentRefunded(PaymentRefundedEvent event){
        orderApplicationService.markPaymentRefunded(event.orderId());
    }


}
