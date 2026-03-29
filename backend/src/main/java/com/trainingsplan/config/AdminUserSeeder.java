package com.trainingsplan.config;

import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_EMAIL:admin@trainingsplan.local}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    public AdminUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.info("Admin user already exists, skipping creation");
            return;
        }

        if ("admin123".equals(adminPassword)) {
            log.warn(">>> Using default admin password! Set ADMIN_PASSWORD environment variable for production! <<<");
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());

        userRepository.save(admin);
        log.info("Created initial admin user: {}", adminUsername);
    }
}
