package com.trainingsplan.controller;

import com.trainingsplan.dto.AuthRequest;
import com.trainingsplan.dto.AuthResponse;
import com.trainingsplan.dto.EmailVerificationRequest;
import com.trainingsplan.dto.MessageResponse;
import com.trainingsplan.dto.RegisterRequest;
import com.trainingsplan.dto.ResendVerificationRequest;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.security.JwtService;
import com.trainingsplan.service.AuditLogService;
import com.trainingsplan.service.EmailService;
import com.trainingsplan.service.TokenBlacklistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int MIN_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final TokenBlacklistService tokenBlacklistService;
    private final com.trainingsplan.security.RateLimitingService rateLimitingService;
    private final com.trainingsplan.service.RefreshTokenService refreshTokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager,
                          EmailService emailService, AuditLogService auditLogService,
                          TokenBlacklistService tokenBlacklistService,
                          com.trainingsplan.security.RateLimitingService rateLimitingService,
                          com.trainingsplan.service.RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.rateLimitingService = rateLimitingService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (rateLimitingService.isRegisterRateLimited(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Too many registration attempts. Please try again later."));
        }
        rateLimitingService.recordRegisterAttempt(clientIp);

        // Password policy validation
        String passwordError = validatePassword(request.password());
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(passwordError));
        }

        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.EMAIL_VERIFICATION_PENDING);
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerificationCode(generateVerificationCode());
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));

        User saved = userRepository.save(user);

        emailService.sendVerificationEmail(saved.getEmail(), saved.getEmailVerificationCode(), false);

        try {
            List<String> adminEmails = userRepository.findByRole(UserRole.ADMIN).stream()
                    .map(User::getEmail)
                    .filter(Objects::nonNull)
                    .toList();
            if (!adminEmails.isEmpty()) {
                emailService.sendAdminNewUserNotification(saved, adminEmails);
            }
        } catch (Exception e) {
            System.err.println("Admin new-user notification failed: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(null, null, saved.getId(), saved.getUsername(), saved.getEmail(),
                        saved.getRole().name(), saved.getStatus().name()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerificationRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (rateLimitingService.isVerificationRateLimited(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Too many verification attempts. Please try again later."));
        }
        rateLimitingService.recordVerificationAttempt(clientIp);

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Ungueltige E-Mail oder Code"));
        }
        if (user.getStatus() != UserStatus.EMAIL_VERIFICATION_PENDING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("E-Mail ist bereits bestaetigt"));
        }
        if (user.getEmailVerificationCode() == null || user.getEmailVerificationExpiresAt() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Kein gueltiger Verifizierungscode vorhanden"));
        }
        if (LocalDateTime.now().isAfter(user.getEmailVerificationExpiresAt())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Verifizierungscode ist abgelaufen"));
        }
        if (!user.getEmailVerificationCode().equals(request.code())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Ungueltige E-Mail oder Code"));
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("E-Mail bestaetigt. Registrierung abgeschlossen."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Kein Benutzer mit dieser E-Mail gefunden"));
        }
        if (user.getStatus() != UserStatus.EMAIL_VERIFICATION_PENDING) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("E-Mail ist bereits bestaetigt"));
        }

        user.setEmailVerificationCode(generateVerificationCode());
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationCode(), true);

        return ResponseEntity.ok(new MessageResponse("Verifizierungscode erneut gesendet."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        if (rateLimitingService.isLoginRateLimited(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Too many login attempts. Please try again later."));
        }
        rateLimitingService.recordLoginAttempt(clientIp);

        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElse(null);
        if (user != null && user.getStatus() != UserStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", user.getStatus().name(),
                    "message", getStatusMessage(user.getStatus()),
                    "email", user.getEmail()
            ));
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User authenticatedUser = (User) authentication.getPrincipal();
        authenticatedUser.setLastLoginAt(LocalDateTime.now());
        userRepository.save(authenticatedUser);
        auditLogService.log(authenticatedUser, AuditAction.LOGIN, null, null,
                Map.of("ip", httpRequest.getRemoteAddr()));
        String token = jwtService.generateToken(authenticatedUser);
        String refreshToken = refreshTokenService.createRefreshToken(authenticatedUser);

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, authenticatedUser.getId(), authenticatedUser.getUsername(),
                authenticatedUser.getEmail(), authenticatedUser.getRole().name(), authenticatedUser.getStatus().name()));
    }

    private String getStatusMessage(UserStatus status) {
        return switch (status) {
            case EMAIL_VERIFICATION_PENDING -> "Bitte bestaetige zuerst deine E-Mail-Adresse.";
            case ADMIN_APPROVAL_PENDING -> "Dein Konto wartet auf Freigabe durch einen Admin.";
            case BLOCKED -> "Dein Konto ist blockiert.";
            case INACTIVE -> "Dein Konto ist inaktiv.";
            case ACTIVE -> "Konto ist aktiv.";
        };
    }

    public record RefreshRequest(String refreshToken) {}

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Refresh token is required"));
        }

        return refreshTokenService.validateAndRotate(request.refreshToken())
                .map(user -> {
                    String newAccessToken = jwtService.generateToken(user);
                    String newRefreshToken = refreshTokenService.createRefreshToken(user);
                    return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, user.getId(),
                            user.getUsername(), user.getEmail(), user.getRole().name(), user.getStatus().name()));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse(null, null, null, null, null, null, "INVALID_REFRESH_TOKEN")));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request, @RequestBody(required = false) RefreshRequest refreshRequest) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                java.util.Date expiration = jwtService.extractClaim(token, io.jsonwebtoken.Claims::getExpiration);
                tokenBlacklistService.blacklist(token, expiration);
                // Also revoke all refresh tokens for this user
                String username = jwtService.extractUsername(token);
                userRepository.findByUsername(username).ifPresent(
                        user -> refreshTokenService.revokeAllForUser(user.getId()));
            } catch (Exception e) {
                // Token already expired or invalid — nothing to blacklist
            }
        }
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    public record ForgotPasswordRequest(String email) {}
    public record ResetPasswordRequest(String token, String newPassword) {}

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        MessageResponse generic = new MessageResponse(
                "Falls die E-Mail-Adresse existiert, wurde eine Anleitung zum Zuruecksetzen versendet.");
        if (request == null || request.email() == null || request.email().isBlank()) {
            return ResponseEntity.ok(generic);
        }
        User user = userRepository.findByEmail(request.email().trim()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(generic);
        }

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        user.setPasswordResetTokenHash(sha256(token));
        user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusMinutes(60));
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        } catch (Exception ignored) {
            // Do not leak failures to the caller
        }
        return ResponseEntity.ok(generic);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()
                || request.newPassword() == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Ungueltiger Link oder abgelaufen"));
        }

        String passwordError = validatePassword(request.newPassword());
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(passwordError));
        }

        String hash = sha256(request.token());
        User user = userRepository.findByPasswordResetTokenHash(hash).orElse(null);
        if (user == null || user.getPasswordResetTokenExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiresAt())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Ungueltiger Link oder abgelaufen"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());

        return ResponseEntity.ok(new MessageResponse("Passwort wurde zurueckgesetzt."));
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long";
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            return "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character";
        }
        return null;
    }
}
