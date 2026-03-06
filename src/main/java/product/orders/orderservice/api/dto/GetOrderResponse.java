package product.orders.orderservice.api.dto;

import product.orders.orderservice.domain.model.OrderProgress;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetOrderResponse(UUID orderId,
                               List<GetOrderItemResponse> items,
                               UUID customerId,
                               String customerEmail,
                               String customerAddress,
                               long totalAmountUSDCents,
                               String currency,
                               String status,
                               OrderProgress progress,
                               PaymentStatus paymentStatus,
                               Instant createdAt) {

    public static GetOrderResponse from(Order order) {
        return new GetOrderResponse(
                order.getOrderId(),
                order.getItems().stream().map(GetOrderItemResponse::from).toList(),
                order.getCustomerDetails().getCustomerId(),
                order.getCustomerDetails().getCustomerEmail(),
                order.getCustomerDetails().getCustomerAddress(),
                order.getTotalAmount().getAmountCents(),
                order.getTotalAmount().getCurrency(),
                order.getStatus().toString(),
                order.getProgress(),
                order.getPaymentStatus(),
                order.getCreatedAt()
        );
    }
}
