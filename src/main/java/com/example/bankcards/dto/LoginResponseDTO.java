package com.example.bankcards.dto;

import lombok.*;

@Builder
public record LoginResponseDTO(
    String token,
    String tokenType,
    Long expiresIn,
    UserDTO user
) {}
