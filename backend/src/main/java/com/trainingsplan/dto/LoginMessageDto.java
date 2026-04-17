package com.trainingsplan.dto;

import com.trainingsplan.entity.LoginMessageTargetGroup;
import com.trainingsplan.entity.LoginMessageTargetType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record LoginMessageDto(
    Long id,
    String title,
    String content,
    boolean published,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    LoginMessageTargetType targetType,
    Set<LoginMessageTargetGroup> targetGroups,
    List<UserSummaryDto> targetUsers
) {}
