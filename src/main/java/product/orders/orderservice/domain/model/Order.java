package product.orders.orderservice.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An order placed by a customer
 */
@Entity
@Table(name = "order_table")
public class Order {
    @Id
    private UUID orderId;

    @JoinColumn(nullable = false, name = "customer_details_id")
    @ManyToOne(cascade = CascadeType.ALL)
    private CustomerDetails customerDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "order_status")
    private OrderStatus status;

    @Embedded
    private Money totalAmount;


    @Column(nullable = false, name = "inventory_status")
    @Enumerated(EnumType.STRING)
    private InventoryStatus inventoryStatus;

    @Column(nullable = false, name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    protected Order() {
    }

    private Order(
            UUID orderId,
            CustomerDetails customerDetails,
            List<OrderItem> items,
            Money totalAmount
    ) {
        this.orderId = Objects.requireNonNull(orderId);
        this.customerDetails = Objects.requireNonNull(customerDetails);
        this.items = List.copyOf(items);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status = OrderStatus.CREATED;
        this.paymentStatus = PaymentStatus.PENDING;
        this.inventoryStatus = InventoryStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        // ensure bidirectional consistency
        this.customerDetails.attachTo(this);
        this.items.forEach(item -> item.attachTo(this));
    }


    // ----------------------------------------------------
    // Factory
    // ----------------------------------------------------

    public static Order create(CustomerDetails customerDetails,
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
                customerDetails,
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
        this.inventoryStatus = InventoryStatus.RESERVED;
        if (paymentStatus == PaymentStatus.COMPLETED) {
            this.status = OrderStatus.CONFIRMED;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the payment as complete. If inventory is reserved, mark the order as confirmed
     */
    public void markPaymentComplete() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot mark payment complete for order " + orderId + " in state " + status
            );
        }
        this.paymentStatus = PaymentStatus.COMPLETED;
        if (inventoryStatus == InventoryStatus.RESERVED) {
            this.status = OrderStatus.CONFIRMED;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the payment as failed for the order. Cancel the order if it is not already confirmed.
     */
    public void markPaymentFailedAndCancel() {
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Confirmed order " + orderId + " cannot be cancelled"
            );
        }
        this.paymentStatus = PaymentStatus.FAILED;
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the inventory as failed for the order. Cancel the order if it is not already confirmed.
     */
    public void markInventoryFailedAndCancel() {
        if (status == OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Confirmed order " + orderId + " cannot be cancelled"
            );
        }
        this.inventoryStatus = InventoryStatus.FAILED;
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the payment status as refunded
     */
    public void markPaymentRefunded(){
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    /**
     * Mark the inventory status as released
     */
    public void markInventoryReleased(){
        this.inventoryStatus = InventoryStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    /**
     * Get the current progress of the order. Surmises the current order progress, payment status, and inventory status
     * into a single enum
     * @return the current progress of the order
     */
    public OrderProgress getProgress(){
        OrderProgress progress = null;
        if(status == OrderStatus.CANCELLED){
            if(paymentStatus == PaymentStatus.COMPLETED){
                progress = OrderProgress.CANCELLED_AWAITING_PAYMENT_REFUND;
            } else if (inventoryStatus == InventoryStatus.FAILED || inventoryStatus == InventoryStatus.RELEASED) {
                progress = OrderProgress.CANCELLED_INVENTORY_RESERVATION_FAILED;
            }else if(paymentStatus == PaymentStatus.FAILED){
                progress = OrderProgress.CANCELLED_PAYMENT_FAILED;
            }
        } else if(paymentStatus == PaymentStatus.PENDING){
            progress = inventoryStatus == InventoryStatus.PENDING
                    ? OrderProgress.AWAITING_PAYMENT_AND_INVENTORY_RESERVATION
                    : OrderProgress.AWAITING_PAYMENT;
        }else if(inventoryStatus == InventoryStatus.PENDING){
            progress = OrderProgress.AWAITING_INVENTORY_RESERVATION;
        } else if (status == OrderStatus.CONFIRMED) {
            progress = OrderProgress.CONFIRMED;
        }

        if(progress == null){
            throw new IllegalStateException("Order is not in a valid progress state");
        }

        return progress;
    }

    // ----------------------------------------------------
    // Getters
    // ----------------------------------------------------

    public UUID getOrderId() {
        return orderId;
    }

    public CustomerDetails getCustomerDetails() {
        return customerDetails;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
