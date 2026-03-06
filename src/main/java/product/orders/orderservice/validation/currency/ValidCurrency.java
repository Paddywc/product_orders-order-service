package product.orders.orderservice.validation.currency;

import jakarta.validation.Constraint;
import org.springframework.messaging.handler.annotation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to validate that a given string is a valid ISO 4217 currency code.
 * This annotation can be applied on fields or method parameters to enforce
 * that the provided value matches one of the officially recognized currency codes.
 * <p>
 * The validation logic is implemented in the {@code ValidCurrencyValidator} class and
 * ensures that the value is non-null, non-blank, and is a valid ISO currency code.
 * <p>
 * The default message for validation failure is "Invalid ISO-4217 currency code".
 * <p>
 * Attributes:
 * - {@code message}: Customizable error message for validation failure.
 * - {@code value}: An optional value that can be associated with the annotation.
 * - {@code groups}: Allows grouping constraints for specific validation flows.
 * - {@code payload}: Can be used to attach additional metadata to a constraint.
 * <p>
 * Usage:
 * This annotation is typically used in DTOs or domain models to validate
 * currency codes, e.g., in order-creation or payment-related operations.
 * <p>
 * Example:
 * Applicable in fields representing currencies such as {@code String currency}
 * in DTOs for request or response validation.
 */
@Constraint(validatedBy = ValidCurrencyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {

    String message() default "Invalid ISO-4217 currency code";

    String value() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
