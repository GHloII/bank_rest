package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.CardFilterDTO;
import com.example.bankcards.dto.CreateCardDTO;
import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.UpdateCardDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
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
    public ResponseEntity<PageResponseDTO<CardDTO>> myCards(@ParameterObject @ModelAttribute CardFilterDTO filter) {
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
    @Operation(
            summary = "Admin: search cards with pagination",
            description = "All query parameters are optional. If you pass no filters, this endpoint returns ALL cards (paginated)."
    )
    public ResponseEntity<PageResponseDTO<CardDTO>> adminSearch(
            @Parameter(description = "Filter by user id. Omit to search across all users.", example = "1")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter by card status. Omit to include any status.", example = "ACTIVE")
            @RequestParam(required = false) CardStatus status,
            @Parameter(description = "Filter by exact last 4 digits of PAN. Omit to include any.", example = "4444")
            @RequestParam(required = false) String last4,
            @Parameter(description = "Filter by owner name substring (case-insensitive). Omit to include any.", example = "john")
            @RequestParam(required = false) String ownerName,
            @Parameter(description = "Include soft-deleted cards. Default: false.", example = "false")
            @RequestParam(required = false) Boolean includeDeleted,
            @Parameter(description = "Page number (0-based). Default: 0.", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size (1..100). Default: 10.", example = "10")
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
