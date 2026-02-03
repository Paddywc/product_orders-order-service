package product.orders.orderservice.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_table")
public class Order {
    @Id
    private UUID orderId;

    @Column(nullable = false, name = "customer_id")
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Embedded
    private Money totalAmount;

    /**
     * Has the inventory been reserved successfully. Once this paymentComplete are true, the order is marked as confirmed.
     */
    @Column(nullable = false)
    private boolean inventoryReserved;

    /**
     * Has the payment been completed successfully. Once this paymentComplete are true, the order is marked as confirmed.
     */
    @Column(nullable = false)
    private boolean paymentComplete;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Order() {
    }

    private Order(
            UUID orderId,
            UUID customerId,
            List<OrderItem> items,
            Money totalAmount
    ) {
        this.orderId = Objects.requireNonNull(orderId);
        this.customerId = Objects.requireNonNull(customerId);
        this.items = List.copyOf(items);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status = OrderStatus.CREATED;
        this.inventoryReserved = false;
        this.paymentComplete = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        // ensure bidirectional consistency
        this.items.forEach(item -> item.attachTo(this));
    }


    // ----------------------------------------------------
    // Factory
    // ----------------------------------------------------

    public static Order create(UUID customerId,
                               List<OrderItem> items,
                               Money totalAmount) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain items");
        }

        // Validate the price
        Money calculatedTotal = items.stream()
                .map(OrderItem::totalPrice)
                .reduce(Money::add)
                .orElseThrow();

        if (!calculatedTotal.equals(totalAmount)) {
            throw new IllegalStateException(
                    "Provided order total does not match calculated total"
            );
        }

        return new Order(
                UUID.randomUUID(),
                customerId,
                items,
                totalAmount
        );
    }



    // ----------------------------------------------------
    // Domain Behavior
    // ----------------------------------------------------

    /**
     * Mark the inventory as reserved. If payment is complete, mark the order as confirmed
     */
    public void markInventoryReserved() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot reserve inventory for order " + orderId + " in state " + status
            );
        }
        this.inventoryReserved = true;
        if(this.paymentComplete){
            this.status = OrderStatus.CONFIRMED;
        }
    }

    public void markPaymentComplete(boolean paymentComplete){
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot mark payment complete for order " + orderId + " in state " + status
            );
        }
        this.paymentComplete = paymentComplete;
        if(paymentComplete && inventoryReserved){
            this.status = OrderStatus.CONFIRMED;
        }
    }



    public void cancel() {
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Confirmed order " + orderId + " cannot be cancelled"
            );
        }
        this.status = OrderStatus.CANCELLED;
    }

    // ----------------------------------------------------
    // Getters
    // ----------------------------------------------------

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public boolean isInventoryReserved() {
        return inventoryReserved;
    }

    public boolean isPaymentComplete() {
        return paymentComplete;
    }

    public List<OrderItem> getItems() {
        return items;
    }


}
