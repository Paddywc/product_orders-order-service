package product.orders.orderservice.api.dto;

import jakarta.validation.constraints.*;
import product.orders.orderservice.validation.currency.ValidCurrency;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty List<CreateOrderItemRequest> items,
        @NotNull UUID customerId,
        @NotNull @Email String customerEmail,

        @NotNull @Size(max=2000) String customerAddress,

        @NotNull Long totalAmountCents,
        @NotNull @ValidCurrency String currency) {


}
