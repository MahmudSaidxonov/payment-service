package uz.example.payment.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.example.payment.payment.entity.PaymentStatusHistory;

import java.util.List;
import java.util.UUID;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, UUID> {
    List<PaymentStatusHistory> findAllByPaymentIdOrderByChangedAtAsc(UUID paymentId);
}
