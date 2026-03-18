package com.trainingsplan.controller;

import com.trainingsplan.dto.AuthRequest;
import com.trainingsplan.dto.AuthResponse;
import com.trainingsplan.dto.EmailVerificationRequest;
import com.trainingsplan.dto.MessageResponse;
import com.trainingsplan.dto.RegisterRequest;
import com.trainingsplan.dto.ResendVerificationRequest;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.security.JwtService;
import com.trainingsplan.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager,
                          EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
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

        emailService.sendSimpleMessage(
                saved.getEmail(),
                "Dein Verifizierungscode",
                "Dein Code lautet: " + saved.getEmailVerificationCode() + "\n\nDer Code ist 15 Minuten gueltig."
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(null, saved.getId(), saved.getUsername(), saved.getEmail(),
                        saved.getRole().name(), saved.getStatus().name()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerificationRequest request) {
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

        emailService.sendSimpleMessage(
                user.getEmail(),
                "Dein Verifizierungscode",
                "Dein neuer Code lautet: " + user.getEmailVerificationCode() + "\n\nDer Code ist 15 Minuten gueltig."
        );

        return ResponseEntity.ok(new MessageResponse("Verifizierungscode erneut gesendet."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElse(null);
        if (user != null && user.getStatus() != UserStatus.ACTIVE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", user.getStatus().name(),
                    "message", getStatusMessage(user.getStatus())
            ));
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User authenticatedUser = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(authenticatedUser);

        return ResponseEntity.ok(new AuthResponse(token, authenticatedUser.getId(), authenticatedUser.getUsername(),
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

    private String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
