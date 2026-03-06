package product.orders.orderservice.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import product.orders.orderservice.domain.model.OrderItem;

import java.util.UUID;

public record GetOrderItemResponse(@NotNull UUID productId,
                                   @NotNull String productName,
                                   @Positive int quantity,
                                   @PositiveOrZero long unitPriceUSDCents) {

    public static GetOrderItemResponse from(OrderItem orderItem) {
        return new GetOrderItemResponse(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice().getAmountCents()
        );
    }
}
