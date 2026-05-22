package com.aiblogstudio.service;

import com.aiblogstudio.model.RefreshToken;
import com.aiblogstudio.model.User;
import com.aiblogstudio.repository.RefreshTokenRepository;
import com.aiblogstudio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for creating, validating, and rotating refresh tokens.
 * Each user has at most one active refresh token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Create a new refresh token for the user, replacing any existing one.
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser(user);
        RefreshToken refreshToken;

        if (existingTokenOpt.isPresent()) {
            refreshToken = existingTokenOpt.get();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        } else {
            refreshToken = RefreshToken.builder()
                    .user(user)
                    .token(UUID.randomUUID().toString())
                    .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                    .build();
        }

        refreshToken = refreshTokenRepository.save(refreshToken);
        log.info("🔄 Refresh token created/updated for user: {}", user.getEmail());
        return refreshToken;
    }

    /**
     * Find a refresh token by its token string.
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verify that a refresh token has not expired.
     * Deletes the token and throws if expired.
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            log.warn("⚠️ Refresh token expired for user: {}", token.getUser().getEmail());
            throw new RuntimeException("Refresh token has expired. Please login again.");
        }
        return token;
    }

    /**
     * Delete all refresh tokens for a user (used during logout / password change).
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
