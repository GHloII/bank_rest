package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDTO {
    private Long id;
    private String panMasked;
    private String ownerName;
    private Integer expiryMonth;
    private Integer expiryYear;
    private CardStatus status;
    private BigDecimal balance;
}
