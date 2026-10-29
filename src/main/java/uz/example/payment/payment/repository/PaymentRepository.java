package uz.example.payment.payment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.example.payment.payment.entity.Payment;
import uz.example.payment.payment.entity.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByUserIdAndIdempotencyKey(Long userId, UUID idempotencyKey);
    Optional<Payment> findByIdAndUserId(UUID id, Long userId);
    Page<Payment> findAllByUserId(Long userId, Pageable pageable);
    Page<Payment> findAllByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);
}
