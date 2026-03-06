package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.CreateCardDTO;
import com.example.bankcards.dto.UpdateCardDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardService;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private CardDTO cardDTO;

    @BeforeEach
    void setUp() {
        cardDTO = CardDTO.builder()
                .id(1L)
                .panMasked("**** **** **** 1234")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void createForUser_ValidBody_ReturnsOk() throws Exception {
        CreateCardDTO dto = CreateCardDTO.builder()
                .pan("1111222233334444")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .build();

        when(cardService.createCardForUser(eq(1L), any(CreateCardDTO.class))).thenReturn(cardDTO);

        mockMvc.perform(post("/cards/admin")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.panMasked").value("**** **** **** 1234"));

        verify(cardService).createCardForUser(eq(1L), any(CreateCardDTO.class));
    }

    @Test
    void createForUser_InvalidBody_ReturnsBadRequest() throws Exception {
        CreateCardDTO dto = CreateCardDTO.builder()
                .pan("")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .build();

        mockMvc.perform(post("/cards/admin")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).createCardForUser(anyLong(), any());
    }

    @Test
    void myCards_ReturnsOk() throws Exception {
        PageResponseDTO<CardDTO> page = PageResponseDTO.of(List.of(cardDTO), 0, 10, 1);
        when(cardService.getMyCards(any())).thenReturn(page);

        mockMvc.perform(get("/cards/me")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(cardService).getMyCards(any());
    }

    @Test
    void myCardById_ReturnsOk() throws Exception {
        when(cardService.getMyCardById(1L)).thenReturn(cardDTO);

        mockMvc.perform(get("/cards/me/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(cardService).getMyCardById(1L);
    }

    @Test
    void requestBlock_ReturnsOk() throws Exception {
        CardDTO blocked = CardDTO.builder()
                .id(1L)
                .panMasked("**** **** **** 1234")
                .ownerName("John Doe")
                .expiryMonth(12)
                .expiryYear(2099)
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("100.00"))
                .build();
        when(cardService.requestBlockMyCard(1L)).thenReturn(blocked);

        mockMvc.perform(post("/cards/me/1/block-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(cardService).requestBlockMyCard(1L);
    }

    @Test
    void update_ValidBody_ReturnsOk() throws Exception {
        UpdateCardDTO dto = UpdateCardDTO.builder()
                .ownerName("New")
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.updateCard(eq(1L), any(UpdateCardDTO.class))).thenReturn(cardDTO);

        mockMvc.perform(patch("/cards/admin/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(cardService).updateCard(eq(1L), any(UpdateCardDTO.class));
    }

    @Test
    void softDelete_ReturnsNoContent() throws Exception {
        doNothing().when(cardService).softDeleteCard(1L);

        mockMvc.perform(delete("/cards/admin/1"))
                .andExpect(status().isNoContent());

        verify(cardService).softDeleteCard(1L);
    }
}
