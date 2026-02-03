package product.orders.orderservice.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record CreateOrderItemRequest(@NotNull UUID productId,
                                     @Positive int quantity,
                                     @PositiveOrZero long unitPriceCents) {


}
