package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardFilterDTO {
    @Schema(description = "Filter by card statuses (repeatable)")
    private List<CardStatus> status;

    @Schema(description = "Filter by owner name (substring match)", example = "John")
    private String ownerName;

    @Schema(description = "Page number (0-based)", example = "0")
    @Builder.Default
    private Integer page = 0;
    
    @Schema(description = "Page size (1..100)", example = "10")
    @Builder.Default
    private Integer size = 10;
    
    public void setPage(Integer page) {
        if (page != null) {
            this.page = Math.max(0, page);
        } else {
            this.page = 0;
        }
    }
    
    public void setSize(Integer size) {
        if (size != null) {
            this.size = Math.min(Math.max(1, size), 100);
        } else {
            this.size = 10;
        }
    }
}
