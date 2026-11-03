package uz.example.payment.payment.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.example.payment.payment.service.PaymentLifecycleService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    private static final int BATCH_SIZE = 10;
    private static final long MIN_DELAY_MS = 2000L;
    private static final long MAX_DELAY_MS = 5000L;

    private final PaymentLifecycleService paymentLifecycleService;

    @Scheduled(fixedDelay = 3000)
    public void processNewPayments() {
        List<UUID> claimedIds = paymentLifecycleService.claimNewPayments(BATCH_SIZE);
        if (claimedIds.isEmpty()) {
            return;
        }

        log.info("Claimed {} payment(s) for background processing: {}", claimedIds.size(), claimedIds);

        simulateProviderDelay();
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Payment processor thread was interrupted during delay. Aborting batch completion.");
            return;
        }


        for (UUID paymentId : claimedIds) {
            try {
                paymentLifecycleService.processAndCompletePayment(paymentId);
            } catch (Exception e) {
                log.error("Error while completing payment id={}", paymentId, e);
            }
        }
    }

    private void simulateProviderDelay() {
        try {
            long delay = ThreadLocalRandom.current().nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Payment processing sleep interrupted");
        }
    }
}
