package uz.example.payment.payment.dto;

public record PaymentCreationResult(
        PaymentResponse response,
        boolean isNew
) {
}
