package com.trainingsplan.dto;

public record AuthResponse(String token, String refreshToken, Long userId, String username, String email, String role, String status) {}
