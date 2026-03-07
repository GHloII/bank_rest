package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Builder;

@Builder
public record CreateCardDTO(
    @Schema(description = "Primary account number (PAN). 16 digits.", example = "1111222233334444")
    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "\\d{16}", message = "PAN must be exactly 16 digits")
    String pan,
    
    @Schema(description = "Card owner name (optional). If empty, it will be derived from user profile.", example = "John Doe")
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    String ownerName,
    
    @Schema(description = "Expiry month (1..12)", example = "12")
    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    Integer expiryMonth,
    
    @Schema(description = "Expiry year (>= 2000)", example = "2099")
    @NotNull(message = "Expiry year is required")
    @Min(value = 2000, message = "Expiry year must be 2000 or later")
    Integer expiryYear
) {}

