package com.trainingsplan.service;

import com.trainingsplan.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private static final String FALLBACK_FROM = "no-reply@smart-trainingsplan.local";

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:${spring.mail.username}}") String fromAddress,
                        @Value("${app.frontend-url:}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        if (!StringUtils.hasText(frontendUrl)) {
            throw new IllegalStateException(
                    "app.frontend-url is not configured. Set the APP_FRONTEND_URL environment variable "
                            + "(e.g. https://pacr.app) - hardcoded fallbacks are not allowed.");
        }
        String trimmed = frontendUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.frontendUrl = trimmed;
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        String effectiveFrom = StringUtils.hasText(fromAddress) ? fromAddress.trim() : FALLBACK_FROM;
        message.setFrom(effectiveFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    /**
     * Sends the email verification message containing both the 6-digit code
     * and a direct link that pre-fills the code on the verify-email screen.
     */
    public void sendVerificationEmail(String to, String code, boolean resend) {
        String link = frontendUrl + "/verify-email?email="
                + URLEncoder.encode(to, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        String intro = resend
                ? "Dein neuer Code lautet: "
                : "Dein Code lautet: ";
        String body = intro + code + "\n\n"
                + "Der Code ist 15 Minuten gueltig.\n\n"
                + "Alternativ kannst du direkt auf diesen Link klicken, um deine E-Mail-Adresse zu bestaetigen:\n"
                + link + "\n";

        sendSimpleMessage(to, "Dein Verifizierungscode", body);
    }

    /**
     * Sends a password reset email containing a link with the reset token.
     */
    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendUrl + "/new-password?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);

        String body = "Du hast eine Zuruecksetzung deines Passworts angefordert.\n\n"
                + "Klicke auf den folgenden Link, um ein neues Passwort zu setzen:\n"
                + link + "\n\n"
                + "Der Link ist 60 Minuten gueltig.\n\n"
                + "Falls du keine Zuruecksetzung angefordert hast, kannst du diese E-Mail ignorieren.\n";

        sendSimpleMessage(to, "Passwort zuruecksetzen", body);
    }

    /**
     * Notifies administrators about a newly registered user.
     */
    public void sendAdminNewUserNotification(User newUser, List<String> adminEmails) {
        if (adminEmails == null || adminEmails.isEmpty()) {
            return;
        }
        String createdAt = newUser.getCreatedAt() != null
                ? newUser.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "-";
        String subject = "[PACR] Neue Registrierung: " + newUser.getUsername();
        String body = "Ein neuer User hat sich registriert.\n\n"
                + "Username: " + newUser.getUsername() + "\n"
                + "E-Mail:   " + newUser.getEmail() + "\n"
                + "Zeit:     " + createdAt + "\n"
                + "User-ID:  " + newUser.getId() + "\n\n"
                + "Status: " + (newUser.getStatus() != null ? newUser.getStatus().name() : "-") + "\n";

        for (String adminEmail : adminEmails) {
            if (StringUtils.hasText(adminEmail)) {
                sendSimpleMessage(adminEmail, subject, body);
            }
        }
    }
}
