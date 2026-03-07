package com.example.bankcards.dto;

import lombok.Builder;

@Builder
public record LoginResponse(
    String token,
    String type,
    Long id,
    String username
) {
    public LoginResponse {
        if (type == null) type = "Bearer";
    }
}
