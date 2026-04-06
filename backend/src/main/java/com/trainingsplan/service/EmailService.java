package com.trainingsplan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final String FALLBACK_FROM = "no-reply@smart-trainingsplan.local";

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:${spring.mail.username}}") String fromAddress,
                        @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
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
        String base = StringUtils.hasText(frontendUrl) ? frontendUrl.trim() : "http://localhost:4200";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String link = base + "/verify-email?email="
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
}
