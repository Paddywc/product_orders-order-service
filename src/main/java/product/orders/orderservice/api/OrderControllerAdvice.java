package product.orders.orderservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import product.orders.orderservice.domain.exception.OrderNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice(assignableTypes = OrderController.class)
public class OrderControllerAdvice {

    // ----------------------------------------------------
    // Validation errors (request body)
    // ----------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex
    ) {
        List<String> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(err ->
                                err.getField() + ": " + err.getDefaultMessage()
                        )
                        .collect(Collectors.toList());

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        "VALIDATION_FAILED",
                        errors
                ));
    }

    // ----------------------------------------------------
    // Illegal arguments (bad client input)
    // ----------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        "INVALID_REQUEST",
                        List.of(ex.getMessage())
                ));
    }


    // ----------------------------------------------------
    // Order not found
    // ----------------------------------------------------

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            OrderNotFoundException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        "ORDER_NOT_FOUND",
                        List.of(ex.getMessage())
                ));
    }

    // ----------------------------------------------------
    // Illegal state (conflict)
    // ----------------------------------------------------

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        "ORDER_STATE_CONFLICT",
                        List.of(ex.getMessage())
                ));
    }

    // ----------------------------------------------------
    // Fallback (unexpected errors)
    // ----------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        "INTERNAL_ERROR",
                        List.of("Unexpected error occurred")
                ));
    }

    // ----------------------------------------------------
    // Error response DTO
    // ----------------------------------------------------

    public record ApiErrorResponse(
            String code,
            List<String> messages,
            Instant timestamp
    ) {
        public static ApiErrorResponse of(
                String code,
                List<String> messages
        ) {
            return new ApiErrorResponse(
                    code,
                    messages,
                    Instant.now()
            );
        }
    }
}