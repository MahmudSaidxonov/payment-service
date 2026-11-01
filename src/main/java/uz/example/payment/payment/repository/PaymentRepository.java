package uz.example.payment.payment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.example.payment.payment.entity.Payment;
import uz.example.payment.payment.entity.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByUserIdAndIdempotencyKey(Long userId, UUID idempotencyKey);
    Optional<Payment> findByIdAndUserId(UUID id, Long userId);
    Page<Payment> findAllByUserId(Long userId, Pageable pageable);
    Page<Payment> findAllByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);

    @Query(
            value = "SELECT * FROM payments WHERE status = 'NEW' ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true
    )
    List<Payment> findAndLockNewPayments(@Param("limit") int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.user.id = :userId")
    Optional<Payment> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") Long userId);
}
