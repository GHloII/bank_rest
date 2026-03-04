package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCardDTO {
    
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    private String ownerName;
    
    private CardStatus status;
}
