package com.example.bankcards.controller;

import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.dto.TopUpRequestDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transfers and transaction history")
public class TransactionController {

    private final TransactionService transactionService;
    private final CardService cardService;

    @PostMapping
    @Operation(summary = "User: top up card balance (deposit)")
    public ResponseEntity<CardDTO> topUp(@Valid @RequestBody TopUpRequestDTO dto) {
        return ResponseEntity.ok(cardService.topUp(dto));
    }

    @PostMapping("/transfer")
    @Operation(summary = "User: transfer between own cards")
    public ResponseEntity<TransactionDTO> transfer(@Valid @RequestBody TransferDTO dto) {
        return ResponseEntity.ok(transactionService.transfer(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "User: my transactions history")
    public ResponseEntity<PageResponseDTO<TransactionDTO>> myTransactions(@RequestParam(required = false) Integer page,
                                                                          @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(transactionService.myTransactions(page, size));
    }

    @GetMapping("/me/card/{cardId}")
    @Operation(summary = "User: my transactions by card")
    public ResponseEntity<PageResponseDTO<TransactionDTO>> myCardTransactions(@PathVariable Long cardId,
                                                                              @RequestParam(required = false) Integer page,
                                                                              @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(transactionService.myCardTransactions(cardId, page, size));
    }
}
