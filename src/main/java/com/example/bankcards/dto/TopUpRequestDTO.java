package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record TopUpRequestDTO(
    @NotNull(message = "Card ID is required")
    Long cardId,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    BigDecimal amount
) {}
