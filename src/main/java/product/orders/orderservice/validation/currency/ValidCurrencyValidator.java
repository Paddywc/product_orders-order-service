package product.orders.orderservice.validation.currency;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidCurrencyValidator implements ConstraintValidator<ValidCurrency, String> {


    private static final Set<String> ISO_CODES =
            Currency.getAvailableCurrencies()
                    .stream()
                    .map(Currency::getCurrencyCode)
                    .collect(Collectors.toUnmodifiableSet());


    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return ISO_CODES.contains(value);
    }
}
