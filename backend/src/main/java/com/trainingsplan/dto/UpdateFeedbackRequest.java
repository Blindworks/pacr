package com.trainingsplan.dto;

import com.trainingsplan.entity.FeedbackStatus;

public record UpdateFeedbackRequest(
    FeedbackStatus status,
    String adminNotes
) {}
