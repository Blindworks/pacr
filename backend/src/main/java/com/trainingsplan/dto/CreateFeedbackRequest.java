package com.trainingsplan.dto;

import com.trainingsplan.entity.FeedbackCategory;

public record CreateFeedbackRequest(
    FeedbackCategory category,
    String subject,
    String message
) {}
