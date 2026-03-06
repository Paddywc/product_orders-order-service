package product.orders.orderservice.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the Money class.
 * Verifies behavior of the add method which sums amounts if currencies match.
 */
class MoneyTest {

    @Test
    void testAdd_whenCurrenciesMatch_AmountsSummed() {
        // Arrange
        Money amount1 = new Money(1000, "USD");
        Money amount2 = new Money(2500, "USD");

        // Act
        Money result = amount1.add(amount2);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3500, result.getAmountCents());
        Assertions.assertEquals("USD", result.getCurrency());
    }

    @Test
    void testAdd_whenCurrenciesDoNotMatch_ThrowsException() {
        // Arrange
        Money amount1 = new Money(1000, "USD");
        Money amount2 = new Money(2500, "EUR");

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> amount1.add(amount2),
                "Expected add() to throw when currencies do not match, but it didn't"
        );

        Assertions.assertEquals(
                "Cannot add money with different currencies: USD vs EUR",
                exception.getMessage()
        );
    }


    @Test
    void testDdd_GivenTwoZeros_SumsToZero() {
        // Arrange
        Money amount1 = new Money(0, "USD");
        Money amount2 = new Money(0, "USD");

        // Act
        Money result = amount1.add(amount2);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getAmountCents());
        Assertions.assertEquals("USD", result.getCurrency());
    }
}