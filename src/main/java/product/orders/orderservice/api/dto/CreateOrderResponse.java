package product.orders.orderservice.api.dto;

import java.util.UUID;

public record CreateOrderResponse(
        UUID orderId,
        UUID customerId,
        String customerEmail,
        String customerAddress,

        long totalAmountCents,
        String currency
) {

    @Override
    public String toString() {
        return "CreateOrderResponse{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerAddress='" + customerAddress + '\'' +
                ", totalAmountUSDCents=" + totalAmountCents +
                ", currency='" + currency + '\'' +
                '}';
    }
}
