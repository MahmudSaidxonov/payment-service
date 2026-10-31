package uz.example.payment.payment.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.example.payment.auth.entity.User;
import uz.example.payment.auth.repository.UserRepository;
import uz.example.payment.common.BusinessException;
import uz.example.payment.errorcode.entity.ErrorCode;
import uz.example.payment.errorcode.service.ErrorCodeService;
import uz.example.payment.payment.dto.*;
import uz.example.payment.payment.entity.Currency;
import uz.example.payment.payment.entity.Payment;
import uz.example.payment.payment.entity.PaymentStatus;
import uz.example.payment.payment.entity.PaymentStatusHistory;
import uz.example.payment.payment.repository.PaymentRepository;
import uz.example.payment.payment.repository.PaymentStatusHistoryRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ErrorCodeService errorCodeService;

    @Transactional
    public PaymentCreationResult createPayment(Long userId, String reqIdempotencyKey, CreatePaymentRequest request) {
        UUID idempotencyKey = parseIdempotencyKey(reqIdempotencyKey);
        Currency currency = parseCurrency(request.currency());
        String requestHash = calculateRequestHash(request, currency);

        Optional<Payment> existingPayment =
                paymentRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

        if (existingPayment.isPresent()) {
            Payment existing = existingPayment.get();
            if (!existing.getRequestHash().equals(requestHash)) {
                log.warn("Idempotency-Key conflict for userId={}, key={}", userId, idempotencyKey);
                throw new BusinessException(2008);
            }
            log.info("Idempotent request hit: returning existing paymentId={} for key={}",
                    existing.getId(), idempotencyKey);
            return new PaymentCreationResult(toDto(existing), false);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(1003));

        Payment payment = Payment.builder()
                .user(user)
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .status(PaymentStatus.NEW)
                .amount(request.amount())
                .currency(currency)
                .receiverAccount(request.receiverAccount())
                .description(request.description())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        PaymentStatusHistory initialHistory = PaymentStatusHistory.builder()
                .payment(savedPayment)
                .status(PaymentStatus.NEW)
                .changedAt(savedPayment.getCreatedAt())
                .build();
        paymentStatusHistoryRepository.save(initialHistory);

        log.info("Created new payment: id={}, userId={}, amount={} {}, receiver={}",
                savedPayment.getId(), userId, savedPayment.getAmount(), savedPayment.getCurrency(), maskAccount(savedPayment.getReceiverAccount()));

        return new PaymentCreationResult(toDto(savedPayment), true);
    }

    public PaymentDetailsResponse getPaymentById(Long userId, UUID paymentId, String langParam, String acceptLanguage) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new BusinessException(2006));

        List<PaymentDetailsResponse.StatusHistoryItem> history = paymentStatusHistoryRepository
                .findAllByPaymentIdOrderByChangedAtAsc(payment.getId())
                .stream()
                .map(h -> new PaymentDetailsResponse.StatusHistoryItem(h.getStatus(), h.getChangedAt()))
                .toList();

        String errorMessage = null;
        if (payment.getStatus() == PaymentStatus.FAILED && payment.getErrorCode() != null) {
            String lang = errorCodeService.resolveLanguage(langParam, acceptLanguage);
            ErrorCode ec = errorCodeService.getEntityByCode(payment.getErrorCode());
            errorMessage = ec.getMessageByLang(lang);
        }

        return new PaymentDetailsResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReceiverAccount(),
                payment.getDescription(),
                payment.getErrorCode(),
                errorMessage,
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                history
        );
    }

    public PaymentPageResponse getUserPayments(Long userId, int page, int size, String statusStr) {
        if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(9001);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> paymentPage;

        if (statusStr != null && !statusStr.isBlank()) {
            PaymentStatus status = parseStatus(statusStr);
            paymentPage = paymentRepository.findAllByUserIdAndStatus(userId, status, pageable);
        } else {
            paymentPage = paymentRepository.findAllByUserId(userId, pageable);
        }

        List<PaymentResponse> content = paymentPage.getContent().stream()
                .map(this::toDto)
                .toList();

        return new PaymentPageResponse(
                content,
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements(),
                paymentPage.getTotalPages()
        );
    }


    private UUID parseIdempotencyKey(String reqKey) {
        if (reqKey == null || reqKey.isBlank()) {
            throw new BusinessException(2007);
        }
        try {
            return UUID.fromString(reqKey.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(2007);
        }
    }

    private Currency parseCurrency(String reqCurrency) {
        try {
            return Currency.valueOf(reqCurrency.trim().toUpperCase());
        } catch ( Exception e) {
            throw new BusinessException(2002);
        }
    }

    private PaymentStatus parseStatus(String reqStatus) {
        try {
            return PaymentStatus.valueOf(reqStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(9001);
        }
    }
    private String calculateRequestHash(CreatePaymentRequest request, Currency currency) {
        String normalizedAmount = request.amount().stripTrailingZeros().toPlainString();
        String desc = request.description() == null ? "" : request.description();
        String payload = normalizedAmount + "|" + currency.name() + "|" + request.receiverAccount() + "|" + desc;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }

    public static String maskAccount(String account) {
        if (account == null || account.length() < 8) {
            return "****";
        }
        return account.substring(0, 4) + "****" + account.substring(account.length() - 4);
    }


    private PaymentResponse toDto(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getStatus(),
                p.getAmount(),
                p.getCurrency(),
                p.getReceiverAccount(),
                p.getDescription(),
                p.getErrorCode(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

}
