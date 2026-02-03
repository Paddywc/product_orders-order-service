package product.orders.orderservice.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import product.orders.orderservice.api.dto.CreateOrderRequest;
import product.orders.orderservice.api.dto.CreateOrderResponse;
import product.orders.orderservice.application.OrderApplicationService;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping()
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<OrderItem> items =
                request.items().stream()
                        .map(i -> new OrderItem(
                                i.productId(),
                                i.quantity(),
                                new Money(
                                        i.unitPriceCents(),
                                        request.currency()
                                )
                        ))
                        .collect(Collectors.toList());

        Money totalAmount = new Money(
                request.totalAmountCents(),
                request.currency()
        );

        UUID orderId = orderApplicationService.createOrder(
                request.customerId(),
                items,
                totalAmount
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateOrderResponse(
                        orderId,
                        request.customerId(),
                        request.totalAmountCents(),
                        request.currency()
                ));
    }
}
