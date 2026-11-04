package uz.example.payment.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uz.example.payment.payment.dto.PaymentWebhookPayload;
import uz.example.payment.payment.entity.Payment;

@Slf4j
@Service
public class WebhookService {
    private final RestClient restClient = RestClient.create();

    @Value("${app.webhook.url:}")
    private String webhookUrl;

    public void sendPaymentResultWebhook(Payment payment) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        PaymentWebhookPayload payload = new PaymentWebhookPayload(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency().name(),
                payment.getErrorCode(),
                payment.getUpdatedAt()
        );

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Webhook sent successfully: paymentId={}, status={}",
                    payment.getId(), payment.getStatus());
        } catch (Exception e) {
            log.warn("Failed to deliver webhook for paymentId={}: {}",
                    payment.getId(), e.getMessage());
        }
    }
}
