package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CardDTO(
    Long id,
    String panMasked,
    String ownerName,
    Integer expiryMonth,
    Integer expiryYear,
    CardStatus status,
    BigDecimal balance
) {}
