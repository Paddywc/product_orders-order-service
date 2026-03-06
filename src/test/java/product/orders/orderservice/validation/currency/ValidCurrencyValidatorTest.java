package product.orders.orderservice.validation.currency;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test class for the {@link ValidCurrencyValidator} which validates if a given string
 * is a valid ISO 4217 currency code.
 */
class ValidCurrencyValidatorTest {



    @Test
    void testIsValid_withValidCurrencyCode_IsValid() {
        // Arrange
        ValidCurrencyValidator validator = new ValidCurrencyValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        String validCurrencyCode = "USD";

        // Act
        boolean result = validator.isValid(validCurrencyCode, context);

        // Assert
        assertTrue(result, "Expected 'USD' to be a valid currency code");
    }

    @Test
    void testIsValid_withInvalidCurrencyCode_NotValid() {
        // Arrange
        ValidCurrencyValidator validator = new ValidCurrencyValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        String invalidCurrencyCode = "XYZ";

        // Act
        boolean result = validator.isValid(invalidCurrencyCode, context);

        // Assert
        assertFalse(result, "Expected 'XYZ' to be an invalid currency code");
    }

    @Test
    void testIsValid_withNullValue_NotValid() {
        // Arrange
        ValidCurrencyValidator validator = new ValidCurrencyValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        String nullValue = null;

        // Act
        boolean result = validator.isValid(nullValue, context);

        // Assert
        assertFalse(result, "Expected null to be invalid");
    }

    @Test
    void testIsValid_withBlankValue_NotValid() {
        // Arrange
        ValidCurrencyValidator validator = new ValidCurrencyValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        String blankValue = "   ";

        // Act
        boolean result = validator.isValid(blankValue, context);

        // Assert
        assertFalse(result, "Expected blank string to be invalid");
    }

    @Test
    void testIsValid_withLowercaseValidCurrencyCode_NotValid() {
        // Arrange
        ValidCurrencyValidator validator = new ValidCurrencyValidator();
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        String lowercaseCurrencyCode = "usd";

        // Act
        boolean result = validator.isValid(lowercaseCurrencyCode, context);

        // Assert
        assertFalse(result, "Expected 'usd' to be invalid, as valid ISO codes are case-sensitive");
    }
}