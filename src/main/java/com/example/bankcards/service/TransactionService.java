package com.example.bankcards.service;

import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.entity.TransactionType;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.util.CardExpiryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final CardExpiryUtil cardExpiryUtil;

    @Transactional
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TransactionDTO transfer(TransferDTO dto) {
        Long userId = getCurrentUserId();

        if (dto.getFromCardId().equals(dto.getToCardId())) {
            throw new BadRequestException("From card and to card must be different");
        }

        Transaction existing = transactionRepository.findByIdempotencyKey(dto.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            ensureTransactionBelongsToUser(existing, userId);
            return toDto(existing);
        }

        Transaction tx = Transaction.builder()
                .fromCardId(dto.getFromCardId())
                .toCardId(dto.getToCardId())
                .amount(dto.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .idempotencyKey(dto.getIdempotencyKey())
                .build();

        try {
            tx = transactionRepository.save(tx);
        } catch (DataIntegrityViolationException e) {
            Transaction raced = transactionRepository.findByIdempotencyKey(dto.getIdempotencyKey())
                    .orElseThrow(() -> new ConflictException("Idempotency key conflict"));
            ensureTransactionBelongsToUser(raced, userId);
            return toDto(raced);
        }

        Card from = cardRepository.findByIdAndUserId(dto.getFromCardId(), userId)
                .orElseThrow(() -> new NotFoundException("From card not found"));
        Card to = cardRepository.findByIdAndUserId(dto.getToCardId(), userId)
                .orElseThrow(() -> new NotFoundException("To card not found"));

        applyExpiredIfNeeded(from);
        applyExpiredIfNeeded(to);

        validateCardForTransfer(from, "From");
        validateCardForTransfer(to, "To");

        BigDecimal amount = dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        if (from.getBalance().compareTo(amount) < 0) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw new ConflictException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        cardRepository.save(from);
        cardRepository.save(to);

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setProcessedAt(LocalDateTime.now());
        tx = transactionRepository.save(tx);

        log.debug("Transfer success txId={} fromCardId={} toCardId={} amount={} userId={}", tx.getId(), from.getId(), to.getId(), amount, userId);

        return toDto(tx);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public PageResponseDTO<TransactionDTO> myTransactions(Integer page, Integer size) {
        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page != null ? Math.max(0, page) : 0,
                size != null ? Math.min(Math.max(1, size), 100) : 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Transaction> result = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponseDTO.of(result.getContent().stream().map(this::toDto).toList(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public PageResponseDTO<TransactionDTO> myCardTransactions(Long cardId, Integer page, Integer size) {
        Long userId = getCurrentUserId();

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        Pageable pageable = PageRequest.of(page != null ? Math.max(0, page) : 0,
                size != null ? Math.min(Math.max(1, size), 100) : 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Transaction> result = transactionRepository.findByCardIdOrderByCreatedAtDesc(card.getId(), pageable);
        return PageResponseDTO.of(result.getContent().stream().map(this::toDto).toList(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private void validateCardForTransfer(Card card, String label) {
        if (card.getDeletedAt() != null) {
            throw new BadRequestException(label + " card is deleted");
        }
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ConflictException(label + " card is blocked");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new ConflictException(label + " card is expired");
        }
    }

    private void applyExpiredIfNeeded(Card card) {
        if (card.getDeletedAt() != null) {
            return;
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            return;
        }
        if (cardExpiryUtil.isExpired(card.getExpiryMonth(), card.getExpiryYear())) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
        }
    }

    private void ensureTransactionBelongsToUser(Transaction tx, Long userId) {
        boolean belongs = transactionRepository.findByIdAndUserId(tx.getId(), userId).isPresent();
        if (!belongs) {
            throw new NotFoundException("Transaction not found");
        }
    }

    private TransactionDTO toDto(Transaction tx) {
        return TransactionDTO.builder()
                .id(tx.getId())
                .fromCardId(tx.getFromCardId())
                .toCardId(tx.getToCardId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        throw new BadRequestException("Invalid authentication principal");
    }
}
