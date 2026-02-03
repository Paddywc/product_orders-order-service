package product.orders.orderservice.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import product.orders.orderservice.validation.currency.ValidCurrency;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty List<CreateOrderItemRequest> items,
        @NotNull UUID customerId,
        @NotNull Long totalAmountCents,
        @NotNull @ValidCurrency String currency) {


}
