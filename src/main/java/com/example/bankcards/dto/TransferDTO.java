package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransferDTO(
    @NotNull(message = "From card ID is required")
    Long fromCardId,
    
    @NotNull(message = "To card ID is required")
    Long toCardId,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount,
    
    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Idempotency key can only contain letters, numbers, underscores and hyphens")
    String idempotencyKey
) {}
