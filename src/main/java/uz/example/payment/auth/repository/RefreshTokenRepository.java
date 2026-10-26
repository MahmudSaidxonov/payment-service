package uz.example.payment.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.example.payment.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
}
