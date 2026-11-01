package uz.example.payment.payment.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uz.example.payment.payment.entity.Payment;
import uz.example.payment.payment.entity.PaymentStatus;
import uz.example.payment.payment.entity.PaymentStatusHistory;
import uz.example.payment.payment.repository.PaymentRepository;
import uz.example.payment.payment.repository.PaymentStatusHistoryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLifecycleService {

    private static final BigDecimal MAX_SUCCESS_AMOUNT = new BigDecimal("50000000.00");

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository historyRepository;

    @Transactional
    public List<UUID> claimNewPayments(int batchSize) {
        List<Payment> newPayments = paymentRepository.findAndLockNewPayments(batchSize);

        for (Payment payment : newPayments) {
            changeStatus(payment, PaymentStatus.PROCESSING, null);
        }

        return newPayments.stream()
                .map(Payment::getId)
                .toList();
    }

    @Transactional
    public void processAndCompletePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.PROCESSING) {
            return;
        }

        if (payment.getAmount().compareTo(MAX_SUCCESS_AMOUNT) > 0) {
            changeStatus(payment, PaymentStatus.FAILED, 2003);
        } else if (payment.getReceiverAccount().endsWith("0000")) {
            changeStatus(payment, PaymentStatus.FAILED, 2004);
        } else if (payment.getReceiverAccount().endsWith("9999")) {
            changeStatus(payment, PaymentStatus.FAILED, 2005);
        } else {
            changeStatus(payment, PaymentStatus.SUCCESS, null);
        }
    }

    public void changeStatus(Payment payment, PaymentStatus nextStatus, Integer errorCode) {
        PaymentStatus currentStatus = payment.getStatus();
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "Invalid payment status transition: " + currentStatus + " -> " + nextStatus
            );
        }

        Instant now = Instant.now();
        payment.setStatus(nextStatus);
        payment.setErrorCode(errorCode);
        payment.setUpdatedAt(now);
        paymentRepository.save(payment);

        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .payment(payment)
                .status(nextStatus)
                .changedAt(now)
                .build();
        historyRepository.save(history);

        log.info("Payment status changed: id={}, newStatus={}, errorCode={}",
                payment.getId(), nextStatus, errorCode);
    }
}
