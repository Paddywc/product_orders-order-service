package product.orders.orderservice.api.dto;

import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        UUID customerId,

        long totalAmountCents,
        String currency
) {

    @Override
    public String toString() {
        return "CreateOrderResponse{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", totalAmountCents=" + totalAmountCents +
                ", currency='" + currency + '\'' +
                '}';
    }
}
