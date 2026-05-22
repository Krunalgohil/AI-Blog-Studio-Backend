package com.aiblogstudio.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async email service for sending OTP verification emails.
 * Uses JavaMailSender with HTML-formatted branded emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send an OTP email asynchronously.
     * Does not block the calling thread.
     */
    @Async
    public void sendOtpEmail(String to, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("AI Blog Studio — Your Verification Code");
            helper.setText(buildOtpEmailHtml(otp, purpose), true);

            mailSender.send(message);
            log.info("📧 OTP email sent to: {} (purpose: {})", to, purpose);
        } catch (MessagingException e) {
            log.error("❌ Failed to send OTP email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildOtpEmailHtml(String otp, String purpose) {
        String title = "Email Verification".equals(purpose)
                ? "Verify Your Email"
                : "Reset Your Password";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #f4f7fa; }
                        .container { max-width: 480px; margin: 40px auto; background: #ffffff; border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); overflow: hidden; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 32px; text-align: center; }
                        .header h1 { color: #fff; margin: 0; font-size: 22px; font-weight: 600; }
                        .body { padding: 32px; text-align: center; }
                        .otp-box { display: inline-block; background: #f0f4ff; border: 2px dashed #667eea; border-radius: 8px; padding: 16px 32px; margin: 24px 0; }
                        .otp-code { font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #2d3748; }
                        .info { color: #718096; font-size: 14px; line-height: 1.6; }
                        .footer { background: #f7fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0; }
                        .footer p { color: #a0aec0; font-size: 12px; margin: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🚀 AI Blog Studio</h1>
                        </div>
                        <div class="body">
                            <h2 style="color: #2d3748; margin-bottom: 8px;">%s</h2>
                            <p class="info">Use the code below to %s. This code expires in <strong>10 minutes</strong>.</p>
                            <div class="otp-box">
                                <span class="otp-code">%s</span>
                            </div>
                            <p class="info">If you didn't request this, you can safely ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 AI Blog Studio. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                title,
                "Email Verification".equals(purpose) ? "verify your email address" : "reset your password",
                otp
        );
    }
}
