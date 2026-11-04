package uz.example.payment.payment.dto;

import uz.example.payment.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentWebhookPayload (
    UUID paymentId,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    Integer errorCode,
    Instant updatedAt
) {}
