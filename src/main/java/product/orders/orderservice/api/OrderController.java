package product.orders.orderservice.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import product.orders.orderservice.api.dto.CreateOrderRequest;
import product.orders.orderservice.api.dto.CreateOrderResponse;
import product.orders.orderservice.api.dto.GetOrderResponse;
import product.orders.orderservice.application.OrderApplicationService;
import product.orders.orderservice.domain.model.CustomerDetails;
import product.orders.orderservice.domain.model.Money;
import product.orders.orderservice.domain.model.Order;
import product.orders.orderservice.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping()
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<OrderItem> items =
                request.items().stream()
                        .map(i -> new OrderItem(
                                i.productId(),
                                i.productName(),
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
                new CustomerDetails(request.customerId(), request.customerEmail(), request.customerAddress()),
                items,
                totalAmount
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateOrderResponse(
                        orderId,
                        request.customerId(),
                        request.customerEmail(),
                        request.customerAddress(),
                        request.totalAmountCents(),
                        request.currency()
                ));
    }

    @GetMapping("/{orderId}")
    public GetOrderResponse getOrder(@PathVariable UUID orderId) {
        Order order = orderApplicationService.getOrder(orderId);
        return GetOrderResponse.from(order);
    }

    @GetMapping("/customer/{customerId}")
    public List<GetOrderResponse> getCustomerOrders(@PathVariable UUID customerId) {
        List<Order> orders = orderApplicationService.getCustomerOrders(customerId);
        return orders.stream()
                .map(GetOrderResponse::from)
                .collect(Collectors.toList());
    }
}
