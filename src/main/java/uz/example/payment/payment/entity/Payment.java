package uz.example.payment.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.example.payment.auth.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payments_user_idempotency",
                        columnNames = {"user_id", "idempotency_id"}
                )
        },
        indexes = {
                @Index(name = "idx_payments_user_created_at", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_payments_user_status_created_at", columnList = "user_id, status, created_at DESC"),
                @Index(name = "idx_payments_status_created_at", columnList = "status, created_at")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private Currency currency;

    @Column(name = "receiver_account", nullable = false, updatable = false, length = 20)
    private String receiverAccount;

    @Column(name = "description", updatable = false, length = 255)
    private String description;

    @Column(name = "error_code")
    private Integer errorCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
