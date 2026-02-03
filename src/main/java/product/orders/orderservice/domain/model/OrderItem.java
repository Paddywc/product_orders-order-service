package product.orders.orderservice.domain.model;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // internal DB PK

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;


    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount"))
    @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency"))
    private Money unitPrice;


    protected OrderItem() {
        // JPA
    }

    public OrderItem(UUID productId, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // ----------------------------------------------------
    // Package-private aggregate wiring
    // ----------------------------------------------------
    void attachTo(Order order) {
        this.order = order;
    }

    // ----------------------------------------------------
    // Domain behavior
    // ----------------------------------------------------


    public Money totalPrice() {
        return new Money(
                unitPrice.getAmountCents() * quantity,
                unitPrice.getCurrency()
        );
    }

    // ----------------------------------------------------
    // Getters
    // ----------------------------------------------------


    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }
}
