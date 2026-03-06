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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardCryptoUtil cardCryptoUtil;

    @Mock
    private CardExpiryUtil cardExpiryUtil;

    @InjectMocks
    private CardService cardService;

    private Card activeCard;

    @BeforeEach
    void setUp() {
        activeCard = Card.builder()
                .id(10L)
                .userId(1L)
                .encryptedPan("enc")
                .panLast4("1234")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private MockedStatic<SecurityContextHolder> mockUserContext(Long userId) {
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
    void createCardForUser_Success_EncryptsAndSaves() {
        CreateCardDTO dto = CreateCardDTO.builder()
                .pan("1111222233334444")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .build();

        when(cardCryptoUtil.last4("1111222233334444")).thenReturn("4444");
        when(cardCryptoUtil.encrypt("1111222233334444")).thenReturn("enc-pan");
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            c.setId(1L);
            c.setPanLast4("4444");
            return c;
        });
        when(cardCryptoUtil.maskLast4("4444")).thenReturn("**** **** **** 4444");
        when(cardExpiryUtil.isExpired(12, 2099)).thenReturn(false);

        CardDTO result = cardService.createCardForUser(99L, dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getOwnerName());
        verify(cardRepository).save(any(Card.class));
        verify(cardCryptoUtil).encrypt("1111222233334444");
    }

    @Test
    void createCardForUser_MissingOwnerName_AutofillsFromUser() {
        CreateCardDTO dto = CreateCardDTO.builder()
                .pan("1111222233334444")
                .ownerName(null)
                .expiryMonth(12)
                .expiryYear(2099)
                .build();

        when(cardCryptoUtil.last4("1111222233334444")).thenReturn("4444");
        when(cardCryptoUtil.encrypt("1111222233334444")).thenReturn("enc-pan");

        User user = User.builder()
                .id(99L)
                .username("user99")
                .fullName("John Derived")
                .email("u99@example.com")
                .passwordHash("hash")
                .enabled(true)
                .build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));

        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            c.setId(2L);
            c.setPanLast4("4444");
            return c;
        });
        when(cardCryptoUtil.maskLast4("4444")).thenReturn("**** **** **** 4444");
        when(cardExpiryUtil.isExpired(12, 2099)).thenReturn(false);

        CardDTO result = cardService.createCardForUser(99L, dto);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("John Derived", result.getOwnerName());
        verify(userRepository).findById(99L);
    }

    @Test
    void createCardForUser_NullUserId_ThrowsBadRequest() {
        CreateCardDTO dto = CreateCardDTO.builder()
                .pan("1111222233334444")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .build();

        assertThrows(BadRequestException.class, () -> cardService.createCardForUser(null, dto));
        verify(cardRepository, never()).save(any());
    }

    @Test
    void getMyCards_ReturnsMaskedPanAndPagination() {
        CardFilterDTO filter = new CardFilterDTO();
        filter.setPage(0);
        filter.setSize(10);

        Page<Card> page = new PageImpl<>(List.of(activeCard), PageRequest.of(0, 10), 1);

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(cardRepository.searchUserCards(eq(1L), any(), isNull(), any(), any(Pageable.class))).thenReturn(page);
            when(cardExpiryUtil.isExpired(activeCard.getExpiryMonth(), activeCard.getExpiryYear())).thenReturn(false);
            when(cardCryptoUtil.maskLast4("1234")).thenReturn("**** **** **** 1234");

            PageResponseDTO<CardDTO> result = cardService.getMyCards(filter);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(0, result.getPage());
            assertEquals(10, result.getSize());
            assertEquals(1, result.getTotalElements());
            assertEquals("**** **** **** 1234", result.getContent().get(0).getPanMasked());
        }
    }

    @Test
    void getMyCardById_DeletedCard_NotFound() {
        Card deleted = Card.builder()
                .id(10L)
                .userId(1L)
                .encryptedPan("enc")
                .panLast4("1234")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .deletedAt(LocalDateTime.now())
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(cardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(deleted));
            assertThrows(NotFoundException.class, () -> cardService.getMyCardById(10L));
        }
    }

    @Test
    void requestBlockMyCard_AlreadyBlocked_ReturnsSame() {
        Card blocked = Card.builder()
                .id(10L)
                .userId(1L)
                .encryptedPan("enc")
                .panLast4("1234")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("100.00"))
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(cardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(blocked));
            when(cardExpiryUtil.isExpired(12, 2099)).thenReturn(false);
            when(cardCryptoUtil.maskLast4("1234")).thenReturn("**** **** **** 1234");

            CardDTO result = cardService.requestBlockMyCard(10L);

            assertNotNull(result);
            assertEquals(CardStatus.BLOCKED, result.getStatus());
            verify(cardRepository, never()).save(any());
        }
    }

    @Test
    void requestBlockMyCard_Expired_ThrowsConflict() {
        try (MockedStatic<SecurityContextHolder> mocked = mockUserContext(1L)) {
            when(cardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(activeCard));
            when(cardExpiryUtil.isExpired(activeCard.getExpiryMonth(), activeCard.getExpiryYear())).thenReturn(true);

            assertThrows(ConflictException.class, () -> cardService.requestBlockMyCard(10L));
        }
    }

    @Test
    void updateCard_AdminUpdate_ChangesOwnerAndStatus() {
        UpdateCardDTO dto = UpdateCardDTO.builder()
                .ownerName("New")
                .status(CardStatus.BLOCKED)
                .build();

        when(cardRepository.findById(10L)).thenReturn(Optional.of(activeCard));
        when(cardRepository.save(activeCard)).thenReturn(activeCard);
        when(cardExpiryUtil.isExpired(activeCard.getExpiryMonth(), activeCard.getExpiryYear())).thenReturn(false);
        when(cardCryptoUtil.maskLast4("1234")).thenReturn("**** **** **** 1234");

        CardDTO result = cardService.updateCard(10L, dto);

        assertEquals("New", result.getOwnerName());
        assertEquals(CardStatus.BLOCKED, result.getStatus());
        verify(cardRepository).save(activeCard);
    }

    @Test
    void softDeleteCard_NotFound_Throws() {
        when(cardRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> cardService.softDeleteCard(10L));
    }
}
