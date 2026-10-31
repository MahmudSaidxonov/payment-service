package uz.example.payment.payment.dto;

import uz.example.payment.payment.entity.Currency;
import uz.example.payment.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentDetailsResponse(
        UUID id,
        PaymentStatus status,
        BigDecimal amount,
        Currency currency,
        String receiverAccount,
        String description,
        Integer errorCode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        List<StatusHistoryItem> history
) {
    public record StatusHistoryItem(
            PaymentStatus status,
            Instant changedAt
    ) {
    }
}
