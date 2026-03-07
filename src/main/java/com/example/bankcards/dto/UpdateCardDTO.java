package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.Size;

import lombok.Builder;

@Builder
public record UpdateCardDTO(
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    String ownerName,
    
    CardStatus status
) {}
