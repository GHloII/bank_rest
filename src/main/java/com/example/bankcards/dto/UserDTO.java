package com.example.bankcards.dto;

import lombok.*;

import java.util.Set;

@Builder
public record UserDTO(
    Long id,
    String username,
    String email,
    String fullName,
    Boolean enabled,
    Set<String> roles
) {}
