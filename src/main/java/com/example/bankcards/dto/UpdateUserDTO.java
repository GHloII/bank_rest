package com.example.bankcards.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
public record UpdateUserDTO(
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    String email,
    
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    String fullName,
    
    Boolean enabled
) {}
