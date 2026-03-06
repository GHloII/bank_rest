package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.CardFilterDTO;
import com.example.bankcards.dto.CreateCardDTO;
import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.UpdateCardDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management endpoints")
public class CardController {

    private final CardService cardService;

    @PostMapping("/admin")
    @Operation(summary = "Admin: create card for user")
    public ResponseEntity<CardDTO> createForUser(@RequestParam Long userId, @Valid @RequestBody CreateCardDTO dto) {
        return ResponseEntity.ok(cardService.createCardForUser(userId, dto));
    }

    @GetMapping("/me")
    @Operation(summary = "User: list my cards with filters and pagination")
    public ResponseEntity<PageResponseDTO<CardDTO>> myCards(@ModelAttribute CardFilterDTO filter) {
        return ResponseEntity.ok(cardService.getMyCards(filter));
    }

    @GetMapping("/me/{cardId}")
    @Operation(summary = "User: get my card by id")
    public ResponseEntity<CardDTO> myCardById(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getMyCardById(cardId));
    }

    @PostMapping("/me/{cardId}/block-request")
    @Operation(summary = "User: request card blocking")
    public ResponseEntity<CardDTO> requestBlock(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.requestBlockMyCard(cardId));
    }

    @GetMapping("/admin/search")
    @Operation(summary = "Admin: search cards with pagination")
    public ResponseEntity<PageResponseDTO<CardDTO>> adminSearch(@RequestParam(required = false) Long userId,
                                                                @RequestParam(required = false) CardStatus status,
                                                                @RequestParam(required = false) String last4,
                                                                @RequestParam(required = false) String ownerName,
                                                                @RequestParam(required = false) Boolean includeDeleted,
                                                                @RequestParam(required = false) Integer page,
                                                                @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(cardService.searchAdminCards(userId, status, last4, ownerName, includeDeleted, page, size));
    }

    @PatchMapping("/admin/{cardId}")
    @Operation(summary = "Admin: update card (ownerName/status)")
    public ResponseEntity<CardDTO> update(@PathVariable Long cardId, @Valid @RequestBody UpdateCardDTO dto) {
        return ResponseEntity.ok(cardService.updateCard(cardId, dto));
    }

    @DeleteMapping("/admin/{cardId}")
    @Operation(summary = "Admin: soft delete card")
    public ResponseEntity<Void> softDelete(@PathVariable Long cardId) {
        cardService.softDeleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
