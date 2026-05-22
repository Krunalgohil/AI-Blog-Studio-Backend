package com.aiblogstudio.repository;

import com.aiblogstudio.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for OTP tokens.
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /**
     * Find the most recent unused OTP for the given email.
     */
    Optional<OtpToken> findTopByEmailAndUsedFalseOrderByExpiresAtDesc(String email);

    /**
     * Delete all OTPs for an email (cleanup after successful verification).
     */
    void deleteAllByEmail(String email);
}
