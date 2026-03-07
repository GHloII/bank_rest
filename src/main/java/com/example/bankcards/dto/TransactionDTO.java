package com.example.bankcards.dto;

import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.entity.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionDTO(
    Long id,
    Long fromCardId,
    Long toCardId,
    BigDecimal amount,
    TransactionType type,
    TransactionStatus status,
    LocalDateTime createdAt
) {}
