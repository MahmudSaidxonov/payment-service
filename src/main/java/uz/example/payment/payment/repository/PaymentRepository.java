package uz.example.payment.payment.repository;

import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.example.payment.payment.entity.Payment;
import uz.example.payment.payment.entity.PaymentStatus;

import java.awt.print.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByUserIdAndIdempotencyKey(Long userId, UUID idempotencyKey);
    Optional<Payment> findByIdAndUserId(UUID id, Long userId);
}
