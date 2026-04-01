package com.trainingsplan.service;

import com.trainingsplan.dto.CreateFeedbackRequest;
import com.trainingsplan.dto.FeedbackDto;
import com.trainingsplan.dto.UpdateFeedbackRequest;
import com.trainingsplan.entity.FeedbackStatus;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserFeedback;
import com.trainingsplan.repository.UserFeedbackRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserFeedbackService {

    private final UserFeedbackRepository userFeedbackRepository;

    public UserFeedbackService(UserFeedbackRepository userFeedbackRepository) {
        this.userFeedbackRepository = userFeedbackRepository;
    }

    public FeedbackDto create(CreateFeedbackRequest request, User user) {
        UserFeedback feedback = new UserFeedback();
        feedback.setUser(user);
        feedback.setCategory(request.category());
        feedback.setSubject(request.subject());
        feedback.setMessage(request.message());
        feedback.setStatus(FeedbackStatus.NEW);
        return toDto(userFeedbackRepository.save(feedback));
    }

    public List<FeedbackDto> findAll() {
        return userFeedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public List<FeedbackDto> findByStatus(FeedbackStatus status) {
        return userFeedbackRepository.findAllByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toDto)
                .toList();
    }

    public FeedbackDto findById(Long id) {
        return userFeedbackRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Feedback not found: " + id));
    }

    public FeedbackDto update(Long id, UpdateFeedbackRequest request) {
        UserFeedback feedback = userFeedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found: " + id));
        feedback.setStatus(request.status());
        feedback.setAdminNotes(request.adminNotes());
        return toDto(userFeedbackRepository.save(feedback));
    }

    private FeedbackDto toDto(UserFeedback f) {
        return new FeedbackDto(
                f.getId(),
                f.getUser().getId(),
                f.getUser().getUsername(),
                f.getCategory(),
                f.getSubject(),
                f.getMessage(),
                f.getStatus(),
                f.getAdminNotes(),
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}
