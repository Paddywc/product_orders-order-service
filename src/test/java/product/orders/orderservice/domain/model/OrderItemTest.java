package product.orders.orderservice.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Test class for the OrderItem class.
 * Verifies the functionality of the totalPrice method, which calculates the total price of an order item.
 */
class OrderItemTest {

    @Test
    void testTotalPrice_GivenValidInput_ReturnsCorrectTotalPrice() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Money unitPrice = new Money(500, "USD"); // $5.00
        int quantity = 3; // Quantity = 3
        String productName = "Test product";
        OrderItem orderItem = new OrderItem(productId, productName, quantity, unitPrice);

        // Act
        Money totalPrice = orderItem.totalPrice();

        // Assert
        Assertions.assertNotNull(totalPrice, "Total price should not be null");
        Assertions.assertEquals(1500, totalPrice.getAmountCents(), "Total price cents should match the expected value");
        Assertions.assertEquals("USD", totalPrice.getCurrency(), "Currency should match the currency of unit price");
    }

    @Test
    void testTotalPrice_WhenQuantityIsOneAndUnitPriceIsZero_ReturnsZero() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Money unitPrice = new Money(0, "USD"); // $0.00
        int quantity = 1; // Quantity = 1
        OrderItem orderItem = new OrderItem(productId, "Test product", quantity, unitPrice);

        // Act
        Money totalPrice = orderItem.totalPrice();

        // Assert
        Assertions.assertNotNull(totalPrice, "Total price should not be null");
        Assertions.assertEquals(0, totalPrice.getAmountCents(), "Total price should be zero");
        Assertions.assertEquals("USD", totalPrice.getCurrency(), "Currency should match the currency of unit price");
    }

    @Test
    void totalPrice_GivenLargeQuantity_ReturnsLargeQuantityCorrectly() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Money unitPrice = new Money(1, "USD"); // $0.01
        int quantity = 1_000_000; // Quantity = 1,000,000
        String productName = "Test product";

        OrderItem orderItem = new OrderItem(productId, productName, quantity, unitPrice);

        // Act
        Money totalPrice = orderItem.totalPrice();

        // Assert
        Assertions.assertNotNull(totalPrice, "Total price should not be null");
        Assertions.assertEquals(1_000_000, totalPrice.getAmountCents(), "Total price cents should match the expected value");
        Assertions.assertEquals("USD", totalPrice.getCurrency(), "Currency should match the currency of unit price");
        Assertions.assertEquals(productName, orderItem.getProductName(), "Product name should match the provided value");
    }

    @Test
    void totalPrice_UnitPriceIsNull_ThrowsException() {
        // Arrange
        UUID productId = UUID.randomUUID();
        int quantity = 5;

        // Act & Assert
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> new OrderItem(productId, "Test", quantity, null),
                "Expected constructor to throw when unit price is null, but it didn't"
        );
        Assertions.assertEquals("Unit price must not be null", exception.getMessage(), "Exception message should indicate null unit price");
    }

    @Test
    void testTotalPrice_WhenQuantityIsZero_ThrowsException() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Money unitPrice = new Money(500, "USD"); // $5.00

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(productId, "t name", 0, unitPrice),
                "Expected constructor to throw when quantity is zero or negative, but it didn't"
        );
        Assertions.assertEquals("Quantity must be greater than zero", exception.getMessage(), "Exception message should indicate invalid quantity");
    }

    @Test
    void testTotalPrice_WhenQuantityIsNegative_ThrowsException() {
        // Arrange
        UUID productId = UUID.randomUUID();
        Money unitPrice = new Money(500, "USD"); // $5.00

        // Act & Assert
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(productId, "Tname", -1, unitPrice),
                "Expected constructor to throw when quantity is negative, but it didn't"
        );
        Assertions.assertEquals("Quantity must be greater than zero", exception.getMessage(), "Exception message should indicate invalid quantity");
    }
}