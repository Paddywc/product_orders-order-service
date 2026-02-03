package product.orders.orderservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Embeddable
public class Money {

    @NotNull
    @PositiveOrZero
    @Column(name = "amount_cents")
    private long amountCents;

    @NotNull
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected Money() {
    }

    public Money(long amountCents, String currency) {
        this.amountCents = amountCents;
        this.currency = currency;
    }

    /**
     * Sum the two amounts and throw an error if different currency
     * @param other the money to add to this total
     * @return money with both totals summed together and the common currency
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot add money with different currencies: "
                            + this.currency + " vs " + other.currency
            );
        }

        return new Money(
                this.amountCents + other.amountCents,
                this.currency
        );
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }
}
