package com.trainingsplan.dto;

import com.trainingsplan.entity.LoginMessageTargetGroup;
import com.trainingsplan.entity.LoginMessageTargetType;

import java.util.Set;

public record CreateLoginMessageRequest(
    String title,
    String content,
    LoginMessageTargetType targetType,
    Set<LoginMessageTargetGroup> targetGroups,
    Set<Long> targetUserIds
) {}
