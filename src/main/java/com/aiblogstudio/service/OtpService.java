package com.aiblogstudio.service;

import com.aiblogstudio.model.OtpToken;
import com.aiblogstudio.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service for generating, storing, and validating OTP tokens.
 * OTPs are 6-digit numeric codes with a 10-minute TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;

    /**
     * Generate a 6-digit OTP, store it in the database, and return it.
     */
    @Transactional
    public String generateAndSaveOtp(String email) {
        String otp = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .build();

        otpTokenRepository.save(otpToken);
        log.info("🔐 OTP generated for email: {}", email);
        return otp;
    }

    /**
     * Validate an OTP for the given email.
     * Checks existence, expiry, and used status.
     * Marks the OTP as used if valid.
     */
    @Transactional
    public boolean validateOtp(String email, String otp) {
        return otpTokenRepository
                .findTopByEmailAndUsedFalseOrderByExpiresAtDesc(email)
                .filter(token -> token.getOtp().equals(otp))
                .filter(token -> !token.isExpired())
                .map(token -> {
                    token.setUsed(true);
                    otpTokenRepository.save(token);
                    log.info("✅ OTP validated successfully for email: {}", email);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Clean up all OTP records for an email after successful verification.
     */
    @Transactional
    public void cleanupOtps(String email) {
        otpTokenRepository.deleteAllByEmail(email);
        log.debug("🧹 Cleaned up OTPs for email: {}", email);
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(RANDOM.nextInt(10));
        }
        return otp.toString();
    }
}
