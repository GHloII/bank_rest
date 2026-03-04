package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardFilterDTO {
    private List<CardStatus> status;
    private String ownerName;
    @Builder.Default
    private Integer page = 0;
    
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
