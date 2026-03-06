package product.orders.orderservice.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Details of a customer that placed an order
 */
@Entity
@Table(name="order_customer_details")
public class CustomerDetails {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    /**
     * Unique ID of the user who placed the order (from the auth/ui service)
     */
    @Column(nullable = false, name = "customer_id")
    @NotNull
    private UUID customerId;

    @Column(nullable = false, name = "customer_email")
    @NotNull
    @Email
    private String customerEmail;

    /**
     * Full address of the customer as a string
     */
    @Column(nullable = false, name="customer_address")
    @NotNull
    @Size(max = 2000)
    private String customerAddress;

    /**
     * Orders placed by this customer. Currently, in practice this is limited to one order per customer details as
     * a new custom details is created for each order, but this could change in the future.
     */
    @OneToMany(mappedBy = "customerDetails")
    private List<Order> orders;

    protected CustomerDetails() {
    }

    public CustomerDetails(UUID customerId, String customerEmail, String customerAddress) {
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.customerAddress = customerAddress;
    }

    // ----------------------------------------------------
    // Package-private aggregate wiring
    // ----------------------------------------------------
    void attachTo(Order order) {
        if(this.orders == null) {
            this.orders = new ArrayList<>();
        }
        this.orders.add(order);
    }

    // ----------------------------------------------------
    // Getters
    // ----------------------------------------------------


    public long getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public List<Order> getOrders() {
        return orders;
    }
}
