package com.trainingsplan.service;

import com.trainingsplan.dto.CreateLoginMessageRequest;
import com.trainingsplan.dto.LoginMessageDto;
import com.trainingsplan.entity.LoginMessage;
import com.trainingsplan.entity.LoginMessageSeenLog;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.LoginMessageRepository;
import com.trainingsplan.repository.LoginMessageSeenLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginMessageService {

    private static final Logger log = LoggerFactory.getLogger(LoginMessageService.class);

    private final LoginMessageRepository messageRepo;
    private final LoginMessageSeenLogRepository seenLogRepo;

    public LoginMessageService(LoginMessageRepository messageRepo, LoginMessageSeenLogRepository seenLogRepo) {
        this.messageRepo = messageRepo;
        this.seenLogRepo = seenLogRepo;
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
        return messageRepo.save(msg);
    }

    @Transactional
    public LoginMessage update(Long id, CreateLoginMessageRequest request) {
        LoginMessage msg = messageRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("LoginMessage not found: " + id));
        msg.setTitle(request.title());
        msg.setContent(request.content());
        return messageRepo.save(msg);
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
        List<LoginMessage> published = messageRepo.findByPublishedTrue();
        return published.stream()
            .filter(msg -> !seenLogRepo.existsByUserIdAndLoginMessageId(userId, msg.getId()))
            .map(this::toDto)
            .toList();
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
        return new LoginMessageDto(
            msg.getId(),
            msg.getTitle(),
            msg.getContent(),
            msg.isPublished(),
            msg.getPublishedAt(),
            msg.getCreatedAt()
        );
    }
}
