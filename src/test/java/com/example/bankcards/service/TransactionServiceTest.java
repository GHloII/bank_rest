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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardExpiryUtil cardExpiryUtil;

    @InjectMocks
    private TransactionService transactionService;

    private Card from;
    private Card to;

    @BeforeEach
    void setUp() {
        from = Card.builder()
                .id(1L)
                .userId(1L)
                .encryptedPan("enc")
                .panLast4("1111")
                .ownerName("User")
                .expiryMonth(12)
                .expiryYear(2099)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        to = Card.builder()
                .id(2L)
                .userId(1L)
                .encryptedPan("enc2")
                .panLast4("2222")
                .ownerName("User")
                .expiryMonth(12)
                .expiryYear(2099)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("50.00"))
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private MockedStatic<SecurityContextHolder> mockUserContext(long userId) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                "user",
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(sc.getAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(principal);

        MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class);
        mocked.when(SecurityContextHolder::getContext).thenReturn(sc);
        return mocked;
    }

    @Test
    void transfer_Success_UpdatesBalancesAndMarksSuccess() {
        TransferDTO dto = TransferDTO.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("10.00"))
                .idempotencyKey("k1")
                .build();

        Transaction savedPending = Transaction.builder()
                .id(100L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(dto.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .idempotencyKey("k1")
                .createdAt(LocalDateTime.now())
                .build();

        Transaction savedSuccess = Transaction.builder()
                .id(100L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(dto.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("k1")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(transactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
            when(transactionRepository.save(any(Transaction.class)))
                    .thenReturn(savedPending)
                    .thenReturn(savedSuccess);

            when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(to));

            when(cardExpiryUtil.isExpired(anyInt(), anyInt())).thenReturn(false);

            TransactionDTO result = transactionService.transfer(dto);

            assertNotNull(result);
            assertEquals(TransactionStatus.SUCCESS, result.getStatus());

            assertEquals(new BigDecimal("90.00"), from.getBalance());
            assertEquals(new BigDecimal("60.00"), to.getBalance());

            verify(cardRepository).save(from);
            verify(cardRepository).save(to);
            verify(transactionRepository, times(2)).save(any(Transaction.class));
        }
    }

    @Test
    void transfer_SameCard_ThrowsBadRequest() {
        TransferDTO dto = TransferDTO.builder()
                .fromCardId(1L)
                .toCardId(1L)
                .amount(new BigDecimal("10.00"))
                .idempotencyKey("k1")
                .build();

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            assertThrows(BadRequestException.class, () -> transactionService.transfer(dto));
        }
    }

    @Test
    void transfer_InsufficientFunds_MarksFailedAndThrowsConflict() {
        TransferDTO dto = TransferDTO.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("1000.00"))
                .idempotencyKey("k1")
                .build();

        Transaction savedPending = Transaction.builder()
                .id(100L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(dto.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .idempotencyKey("k1")
                .createdAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(transactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedPending);

            when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(to));

            when(cardExpiryUtil.isExpired(anyInt(), anyInt())).thenReturn(false);

            assertThrows(ConflictException.class, () -> transactionService.transfer(dto));

            verify(transactionRepository, atLeast(2)).save(any(Transaction.class));
        }
    }

    @Test
    void transfer_IdempotencyExisting_ReturnsExisting() {
        TransferDTO dto = TransferDTO.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("10.00"))
                .idempotencyKey("k1")
                .build();

        Transaction existing = Transaction.builder()
                .id(55L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(dto.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("k1")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(transactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.of(existing));
            when(transactionRepository.findByIdAndUserId(55L, 1L)).thenReturn(Optional.of(existing));

            TransactionDTO result = transactionService.transfer(dto);

            assertEquals(55L, result.getId());
            verify(transactionRepository, never()).save(argThat(t -> t.getId() == null));
        }
    }

    @Test
    void transfer_IdempotencyRace_ReturnsRacedTransaction() {
        TransferDTO dto = TransferDTO.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("10.00"))
                .idempotencyKey("k1")
                .build();

        Transaction raced = Transaction.builder()
                .id(77L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(dto.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("k1")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(transactionRepository.findByIdempotencyKey("k1")).thenReturn(Optional.empty())
                    .thenReturn(Optional.of(raced));
            when(transactionRepository.save(any(Transaction.class))).thenThrow(new DataIntegrityViolationException("dup"));
            when(transactionRepository.findByIdAndUserId(77L, 1L)).thenReturn(Optional.of(raced));

            TransactionDTO result = transactionService.transfer(dto);

            assertEquals(77L, result.getId());
        }
    }

    @Test
    void myTransactions_ReturnsPage() {
        Transaction tx = Transaction.builder()
                .id(1L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("1.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey("k")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        Page<Transaction> page = new PageImpl<>(List.of(tx));

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(page);

            PageResponseDTO<TransactionDTO> result = transactionService.myTransactions(0, 10);

            assertEquals(1, result.getContent().size());
        }
    }

    @Test
    void myCardTransactions_CardNotFound_Throws() {
        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(cardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> transactionService.myCardTransactions(10L, 0, 10));
        }
    }
}
