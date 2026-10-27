package uz.example.payment.auth.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.example.payment.auth.dto.*;
import uz.example.payment.auth.entity.RefreshToken;
import uz.example.payment.auth.entity.User;
import uz.example.payment.auth.repository.RefreshTokenRepository;
import uz.example.payment.auth.repository.UserRepository;
import uz.example.payment.common.BusinessException;
import uz.example.payment.security.JwtService;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-ttl}")
    private long refreshTtlSeconds;

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new BusinessException(1004);
        }

        User user = new User();
        user.setUsername(registerRequest.username());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);

        log.info("Registered new user: username={}, id={}", savedUser.getUsername(), savedUser.getId());

        return new RegisterResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getCreatedAt());
    }

    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new BusinessException(1001));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new BusinessException(1001);
        }

        log.info("User logged in successfully: username={}", user.getUsername());

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest refreshRequest) {
        RefreshToken existingToken = refreshTokenRepository.findByToken(refreshRequest.refreshToken())
                .orElseThrow(() -> new BusinessException(1005));

        if (existingToken.isUsed() || existingToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(1005);
        }

        existingToken.setUsed(true);
        refreshTokenRepository.save(existingToken);

        User user = existingToken.getUser();

        log.info("Refreshed tokens for user: username={}", user.getUsername());

        return issueTokens(user);

    }


    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
//        String refreshTokenString = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        String refreshTokenString = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenString);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));
        refreshToken.setUsed(false);
        refreshToken.setCreatedAt(Instant.now());

        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                accessToken,
                refreshTokenString,
                "Bearer",
                jwtService.getAccessTtlSeconds()
        );
    }
}
