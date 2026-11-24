package com.carbontrack.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
