package com.aiblogstudio.controller;

import com.aiblogstudio.dto.ApiResponse;
import com.aiblogstudio.dto.auth.*;
import com.aiblogstudio.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for all authentication endpoints.
 * All endpoints under /api/auth/** are publicly accessible.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user account.
     * Sends an OTP to the provided email for verification.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("📝 Signup request for email: {}", request.getEmail());
        ApiResponse response = authService.signup(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Verify email using the OTP sent during signup.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        log.info("🔐 OTP verification request for email: {}", request.getEmail());
        ApiResponse response = authService.verifyOtp(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Login with email and password.
     * Returns JWT access token and refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("🔑 Login attempt for email: {}", request.getEmail());
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh the access token using a valid refresh token.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<JwtResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        log.info("🔄 Token refresh request");
        JwtResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiate password reset by sending OTP to email.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("📧 Forgot password request for email: {}", request.getEmail());
        ApiResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset password using email OTP verification.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("🔒 Password reset request for email: {}", request.getEmail());
        ApiResponse response = authService.resetPassword(request);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}
