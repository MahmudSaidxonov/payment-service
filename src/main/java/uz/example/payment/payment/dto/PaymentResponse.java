package uz.example.payment.payment.dto;

import uz.example.payment.payment.entity.Currency;
import uz.example.payment.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        PaymentStatus status,
        BigDecimal amount,
        Currency currency,
        String receiverAccount,
        String description,
        Integer errorCode,
        Instant createdAt,
        Instant updatedAt
) {
}
