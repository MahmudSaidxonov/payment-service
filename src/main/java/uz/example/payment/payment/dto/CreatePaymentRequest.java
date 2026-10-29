package uz.example.payment.payment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Must be greater than 0")
        @DecimalMax(value = "100000000.00", message = "Must not exceed 100000000.00")
        @Digits(integer = 9, fraction = 2, message = "Must have at most 2 decimal places")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "ReceiverAccount is required")
        @Pattern(regexp = "^\\d{20}$", message = "Must be a 20-digit number")
        String receiverAccount,

        @Size(max = 255, message = "Must not exceed 255 characters")
        String description
) {
}
