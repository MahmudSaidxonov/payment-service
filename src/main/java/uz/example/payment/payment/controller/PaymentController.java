package uz.example.payment.payment.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.example.payment.payment.dto.*;
import uz.example.payment.payment.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment (
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest createPaymentRequest
            ) {
        PaymentCreationResult result =
                paymentService.createPayment(userId, idempotencyKey, createPaymentRequest);

        HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping("/{id}")
    public PaymentDetailsResponse getPaymentById(
            @AuthenticationPrincipal Long userId,
            @PathVariable UUID id,
            @RequestParam(required = false) String lang,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        return paymentService.getPaymentById(userId, id, lang, acceptLanguage);
    }

    @GetMapping
    public PaymentPageResponse getPayments(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        return paymentService.getUserPayments(userId, page, size, status);
    }
}
