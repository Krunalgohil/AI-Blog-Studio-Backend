package com.aiblogstudio.service;

import com.aiblogstudio.dto.ApiResponse;
import com.aiblogstudio.dto.auth.*;
import com.aiblogstudio.enums.Role;
import com.aiblogstudio.model.RefreshToken;
import com.aiblogstudio.model.User;
import com.aiblogstudio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication service handling signup, OTP verification, login,
 * password reset, and token refresh flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user with disabled status and send OTP for email verification.
     */
    @Transactional
    public ApiResponse signup(SignupRequest request) {
        // Check if email is already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Email is already registered");
        }

        // Create user with disabled status
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.ROLE_USER)
                .enabled(false)
                .build();

        userRepository.save(user);
        log.info("👤 New user registered: {} (awaiting email verification)", request.getEmail());

        // Generate and send OTP
        String otp = otpService.generateAndSaveOtp(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp, "Email Verification");

        return ApiResponse.success("Registration successful! Please check your email for the OTP to verify your account.");
    }

    /**
     * Verify email OTP and enable the user account.
     */
    @Transactional
    public ApiResponse verifyOtp(OtpVerificationRequest request) {
        // Validate OTP
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            return ApiResponse.error("OTP_EXPIRED");
        }

        // Enable user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEnabled(true);
        userRepository.save(user);

        // Cleanup used OTPs
        otpService.cleanupOtps(request.getEmail());

        log.info("✅ Email verified successfully for: {}", request.getEmail());
        return ApiResponse.success("Email verified successfully! You can now login.");
    }

    /**
     * Authenticate user and return JWT access + refresh tokens.
     */
    public JwtResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            log.warn("⚠️ Unverified user attempted login. Resending OTP to: {}", request.getEmail());
            String otp = otpService.generateAndSaveOtp(request.getEmail());
            emailService.sendOtpEmail(request.getEmail(), otp, "Email Verification");
            throw new DisabledException("USER_NOT_VERIFIED");
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("INVALID_CREDENTIALS");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("INVALID_CREDENTIALS"));

        // Generate tokens
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("🔑 User logged in: {} [{}]", user.getEmail(), user.getRole());

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Refresh the access token using a valid refresh token.
     */
    @Transactional
    public JwtResponse refreshToken(TokenRefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = jwtService.generateToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    log.info("🔄 Token refreshed for user: {}", user.getEmail());

                    return JwtResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .tokenType("Bearer")
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .role(user.getRole().name())
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
    }

    /**
     * Initiate forgot password flow by sending OTP to email.
     */
    public ApiResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Always return success to prevent email enumeration
        if (user == null) {
            log.warn("⚠️ Forgot password request for non-existent email: {}", request.getEmail());
            return ApiResponse.success("If an account with this email exists, an OTP has been sent.");
        }

        String otp = otpService.generateAndSaveOtp(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp, "Password Reset");

        log.info("📧 Password reset OTP sent to: {}", request.getEmail());
        return ApiResponse.success("If an account with this email exists, an OTP has been sent.");
    }

    /**
     * Reset password using email OTP verification.
     */
    @Transactional
    public ApiResponse resetPassword(ResetPasswordRequest request) {
        // Validate OTP
        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            return ApiResponse.error("OTP_EXPIRED");
        }

        // Update password
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all refresh tokens for security
        refreshTokenService.deleteByUser(user);

        // Cleanup used OTPs
        otpService.cleanupOtps(request.getEmail());

        log.info("🔒 Password reset successfully for: {}", request.getEmail());
        return ApiResponse.success("Password reset successfully! You can now login with your new password.");
    }
}
