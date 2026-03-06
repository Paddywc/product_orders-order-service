package product.orders.orderservice.domain.model;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

/**
 * A snapshot of a product ordered in an order
 */
@Entity
@Table(name = "order_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_item_order", columnNames = {"order_id", "product_id"})})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // internal DB PK

    @Column(nullable = false, name = "product_id")
    private UUID productId;

    /**
     * Snapshot of the product name at purchase time.
     */
    @Column(nullable = false, name="name_snapshot")
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;


    /**
     * Price of 1 unit of the product.
     */
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price_amount"))
    @AttributeOverride(name = "currency", column = @Column(name = "unit_price_currency"))
    private Money unitPrice;


    protected OrderItem() {
        // JPA
    }

    public OrderItem(UUID productId, String productName, int quantity, Money unitPrice) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        Objects.requireNonNull(unitPrice, "Unit price must not be null");
        this.productId = productId;
        this.productName = productName;
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

    /**
     *
     * @return a snapshot of the name of the product at purchase
     */
    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public Order getOrder() {
        return order;
    }
}
