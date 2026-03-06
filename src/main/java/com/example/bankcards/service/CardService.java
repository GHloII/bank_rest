package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.CardFilterDTO;
import com.example.bankcards.dto.CreateCardDTO;
import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.UpdateCardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.util.CardCryptoUtil;
import com.example.bankcards.util.CardExpiryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardCryptoUtil cardCryptoUtil;
    private final CardExpiryUtil cardExpiryUtil;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CardDTO createCardForUser(Long userId, CreateCardDTO dto) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }

        if (dto == null) {
            throw new BadRequestException("Create payload is required");
        }

        String last4 = cardCryptoUtil.last4(dto.getPan());
        String encryptedPan = cardCryptoUtil.encrypt(dto.getPan());

        String ownerName = dto.getOwnerName();
        if (ownerName == null || ownerName.isBlank()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
            if (user.getFullName() != null && !user.getFullName().isBlank()) {
                ownerName = user.getFullName();
            } else {
                ownerName = user.getUsername();
            }
        }

        Card card = Card.builder()
                .userId(userId)
                .encryptedPan(encryptedPan)
                .panLast4(last4)
                .ownerName(ownerName)
                .expiryMonth(dto.getExpiryMonth())
                .expiryYear(dto.getExpiryYear())
                .status(CardStatus.ACTIVE)
                .build();

        Card saved = cardRepository.save(card);
        log.debug("Created card {} for user {}", saved.getId(), userId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public PageResponseDTO<CardDTO> getMyCards(CardFilterDTO filter) {
        Long userId = getCurrentUserId();
        Pageable pageable = toPageable(filter);

        CardStatus status = null;
        List<CardStatus> statuses = filter != null ? filter.getStatus() : null;
        if (statuses != null && !statuses.isEmpty()) {
            status = statuses.get(0);
        }

        Page<Card> page = cardRepository.searchUserCards(userId, status, null, filter != null ? filter.getOwnerName() : null, pageable);
        page.getContent().forEach(this::applyExpiredIfNeeded);
        return PageResponseDTO.of(page.getContent().stream().map(this::toDto).toList(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public CardDTO getMyCardById(Long cardId) {
        Long userId = getCurrentUserId();
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        applyExpiredIfNeeded(card);
        return toDto(card);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public CardDTO requestBlockMyCard(Long cardId) {
        Long userId = getCurrentUserId();

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (card.getDeletedAt() != null) {
            throw new BadRequestException("Card is deleted");
        }

        applyExpiredIfNeeded(card);
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new ConflictException("Card is expired");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            return toDto(card);
        }

        card.setStatus(CardStatus.BLOCKED);
        Card saved = cardRepository.save(card);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDTO<CardDTO> searchAdminCards(Long userId, CardStatus status, String last4, String ownerName, Boolean includeDeleted, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? Math.max(0, page) : 0, size != null ? Math.min(Math.max(1, size), 100) : 10, Sort.by(Sort.Direction.DESC, "id"));
        boolean incDel = includeDeleted != null && includeDeleted;

        Page<Card> result = cardRepository.searchAdminCards(userId, status, last4, ownerName, incDel, pageable);
        result.getContent().forEach(this::applyExpiredIfNeeded);
        return PageResponseDTO.of(result.getContent().stream().map(this::toDto).toList(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CardDTO updateCard(Long cardId, UpdateCardDTO dto) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (dto.getOwnerName() != null) {
            card.setOwnerName(dto.getOwnerName());
        }
        if (dto.getStatus() != null) {
            card.setStatus(dto.getStatus());
        }

        Card saved = cardRepository.save(card);
        applyExpiredIfNeeded(saved);
        return toDto(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void softDeleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
        card.setDeletedAt(LocalDateTime.now());
        cardRepository.save(card);
    }

    private Pageable toPageable(CardFilterDTO filter) {
        int page = filter != null && filter.getPage() != null ? filter.getPage() : 0;
        int size = filter != null && filter.getSize() != null ? filter.getSize() : 10;
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    private CardDTO toDto(Card card) {
        applyExpiredIfNeeded(card);
        return CardDTO.builder()
                .id(card.getId())
                .panMasked(cardCryptoUtil.maskLast4(card.getPanLast4()))
                .ownerName(card.getOwnerName())
                .expiryMonth(card.getExpiryMonth())
                .expiryYear(card.getExpiryYear())
                .status(card.getStatus())
                .balance(card.getBalance())
                .build();
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

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        throw new BadRequestException("Invalid authentication principal");
    }
}
