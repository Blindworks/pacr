package com.trainingsplan.service;

import com.trainingsplan.dto.CreateLoginMessageRequest;
import com.trainingsplan.dto.LoginMessageDto;
import com.trainingsplan.dto.UserSummaryDto;
import com.trainingsplan.entity.LoginMessage;
import com.trainingsplan.entity.LoginMessageSeenLog;
import com.trainingsplan.entity.LoginMessageTargetGroup;
import com.trainingsplan.entity.LoginMessageTargetType;
import com.trainingsplan.entity.SubscriptionPlan;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserRole;
import com.trainingsplan.repository.LoginMessageRepository;
import com.trainingsplan.repository.LoginMessageSeenLogRepository;
import com.trainingsplan.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class LoginMessageService {

    private static final Logger log = LoggerFactory.getLogger(LoginMessageService.class);

    private final LoginMessageRepository messageRepo;
    private final LoginMessageSeenLogRepository seenLogRepo;
    private final UserRepository userRepo;

    public LoginMessageService(LoginMessageRepository messageRepo,
                               LoginMessageSeenLogRepository seenLogRepo,
                               UserRepository userRepo) {
        this.messageRepo = messageRepo;
        this.seenLogRepo = seenLogRepo;
        this.userRepo = userRepo;
    }

    public List<LoginMessage> findAll() {
        return messageRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public LoginMessage create(CreateLoginMessageRequest request, User creator) {
        LoginMessage msg = new LoginMessage();
        msg.setTitle(request.title());
        msg.setContent(request.content());
        msg.setCreatedBy(creator);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setPublished(false);
        applyTargeting(msg, request);
        return messageRepo.save(msg);
    }

    @Transactional
    public LoginMessage update(Long id, CreateLoginMessageRequest request) {
        LoginMessage msg = messageRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("LoginMessage not found: " + id));
        msg.setTitle(request.title());
        msg.setContent(request.content());
        applyTargeting(msg, request);
        return messageRepo.save(msg);
    }

    private void applyTargeting(LoginMessage msg, CreateLoginMessageRequest request) {
        LoginMessageTargetType type = request.targetType() != null
            ? request.targetType()
            : LoginMessageTargetType.ALL;
        msg.setTargetType(type);

        switch (type) {
            case ALL -> {
                msg.getTargetGroups().clear();
                msg.getTargetUsers().clear();
            }
            case GROUPS -> {
                Set<LoginMessageTargetGroup> groups = request.targetGroups();
                if (groups == null || groups.isEmpty()) {
                    throw new IllegalArgumentException("targetGroups must not be empty when targetType=GROUPS");
                }
                msg.getTargetGroups().clear();
                msg.getTargetGroups().addAll(groups);
                msg.getTargetUsers().clear();
            }
            case USERS -> {
                Set<Long> userIds = request.targetUserIds();
                if (userIds == null || userIds.isEmpty()) {
                    throw new IllegalArgumentException("targetUserIds must not be empty when targetType=USERS");
                }
                List<User> users = userRepo.findAllById(userIds);
                if (users.size() != userIds.size()) {
                    throw new IllegalArgumentException("One or more target user IDs not found");
                }
                msg.getTargetUsers().clear();
                msg.getTargetUsers().addAll(users);
                msg.getTargetGroups().clear();
            }
        }
    }

    @Transactional
    public LoginMessage publish(Long id) {
        LoginMessage msg = messageRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("LoginMessage not found: " + id));
        msg.setPublished(true);
        msg.setPublishedAt(LocalDateTime.now());
        return messageRepo.save(msg);
    }

    @Transactional
    public LoginMessage unpublish(Long id) {
        LoginMessage msg = messageRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("LoginMessage not found: " + id));
        msg.setPublished(false);
        msg.setPublishedAt(null);
        return messageRepo.save(msg);
    }

    @Transactional
    public void delete(Long id) {
        messageRepo.deleteById(id);
    }

    public List<LoginMessageDto> findPendingForUser(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        List<LoginMessage> published = messageRepo.findByPublishedTrue();
        return published.stream()
            .filter(msg -> isTargeted(msg, user))
            .filter(msg -> !seenLogRepo.existsByUserIdAndLoginMessageId(userId, msg.getId()))
            .map(this::toDto)
            .toList();
    }

    private boolean isTargeted(LoginMessage msg, User user) {
        return switch (msg.getTargetType()) {
            case ALL -> true;
            case GROUPS -> msg.getTargetGroups().stream().anyMatch(g -> matchesGroup(g, user));
            case USERS -> msg.getTargetUsers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        };
    }

    private boolean matchesGroup(LoginMessageTargetGroup group, User user) {
        return switch (group) {
            case PRO -> user.getSubscriptionPlan() == SubscriptionPlan.PRO;
            case FREE -> user.getSubscriptionPlan() == SubscriptionPlan.FREE;
            case TRAINER -> user.getRole() == UserRole.TRAINER;
        };
    }

    @Transactional
    public void markAsSeen(Long userId, Long messageId, User user) {
        if (seenLogRepo.existsByUserIdAndLoginMessageId(userId, messageId)) {
            return;
        }
        LoginMessage msg = messageRepo.findById(messageId)
            .orElseThrow(() -> new RuntimeException("LoginMessage not found: " + messageId));
        LoginMessageSeenLog seenLog = new LoginMessageSeenLog();
        seenLog.setUser(user);
        seenLog.setLoginMessage(msg);
        seenLog.setSeenAt(LocalDateTime.now());
        seenLogRepo.save(seenLog);
    }

    public LoginMessageDto toDto(LoginMessage msg) {
        List<UserSummaryDto> users = msg.getTargetUsers().stream()
            .map(u -> new UserSummaryDto(u.getId(), u.getUsername(), u.getEmail()))
            .toList();
        return new LoginMessageDto(
            msg.getId(),
            msg.getTitle(),
            msg.getContent(),
            msg.isPublished(),
            msg.getPublishedAt(),
            msg.getCreatedAt(),
            msg.getTargetType(),
            new HashSet<>(msg.getTargetGroups()),
            users
        );
    }
}
