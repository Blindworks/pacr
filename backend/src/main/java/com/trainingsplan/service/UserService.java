package com.trainingsplan.service;

import com.trainingsplan.entity.UserRole;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.port.ImageStoragePort;
import com.trainingsplan.repository.UserRepository;
import com.trainingsplan.security.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ImageStoragePort imageStoragePort;

    public UserService(UserRepository userRepository, SecurityUtils securityUtils, ImageStoragePort imageStoragePort) {
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.imageStoragePort = imageStoragePort;
    }

    public User createUser(String username, String email) {
        User user = new User(username, email, LocalDateTime.now());
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User updateUser(Long id, String username, String email,
                           String firstName, String lastName,
                           LocalDate dateOfBirth, Integer heightCm, Double weightKg,
                           Integer maxHeartRate, Integer hrRest, String gender, String status,
                           Integer dwdRegionId, boolean asthmaTrackingEnabled) {
        User user = findById(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDateOfBirth(dateOfBirth);
        user.setHeightCm(heightCm);
        user.setWeightKg(weightKg);
        user.setMaxHeartRate(maxHeartRate);
        user.setHrRest(hrRest);
        user.setGender(gender);
        if (status != null && !status.isBlank()) {
            user.setStatus(UserStatus.valueOf(status));
        }
        user.setDwdRegionId(dwdRegionId);
        user.setAsthmaTrackingEnabled(asthmaTrackingEnabled);
        return userRepository.save(user);
    }

    public User updatePaceZoneReference(Long userId, Double distanceM, Integer timeSeconds,
                                        String label, Integer thresholdPace) {
        User user = findById(userId);
        user.setPaceRefDistanceM(distanceM);
        user.setPaceRefTimeSeconds(timeSeconds);
        user.setPaceRefLabel(label);
        user.setThresholdPaceSecPerKm(thresholdPace);
        return userRepository.save(user);
    }

    public void uploadProfileImage(Long targetUserId, MultipartFile file) {
        User currentUser = requireAuthenticatedUser();
        if (!currentUser.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        User targetUser = findById(targetUserId);
        String newFilename = imageStoragePort.store(file);
        String oldFilename = targetUser.getProfileImageFilename();

        targetUser.setProfileImageFilename(newFilename);
        userRepository.save(targetUser);

        if (oldFilename != null && !oldFilename.isBlank() && !oldFilename.equals(newFilename)) {
            imageStoragePort.delete(oldFilename);
        }
    }

    public ProfileImageData loadProfileImage(Long targetUserId) {
        User currentUser = requireAuthenticatedUser();
        if (!currentUser.getId().equals(targetUserId) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        User targetUser = findById(targetUserId);
        String filename = targetUser.getProfileImageFilename();
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }

        Resource resource = imageStoragePort.load(filename);
        String contentType = "application/octet-stream";
        try {
            String detectedType = Files.probeContentType(resource.getFile().toPath());
            if (detectedType != null && !detectedType.isBlank()) {
                contentType = detectedType;
            }
        } catch (IOException ignored) {
        }

        return new ProfileImageData(resource, contentType);
    }

    private User requireAuthenticatedUser() {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return currentUser;
    }

    public record ProfileImageData(Resource resource, String contentType) {
    }
}
