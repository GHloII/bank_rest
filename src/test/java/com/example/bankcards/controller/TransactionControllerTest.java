package com.example.bankcards.controller;

import com.example.bankcards.dto.PageResponseDTO;
import com.example.bankcards.dto.TransactionDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.entity.TransactionStatus;
import com.example.bankcards.entity.TransactionType;
import com.example.bankcards.service.TransactionService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private TransactionDTO tx;

    @BeforeEach
    void setUp() {
        tx = TransactionDTO.builder()
                .id(1L)
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("10.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void transfer_ValidBody_ReturnsOk() throws Exception {
        TransferDTO dto = TransferDTO.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("10.00"))
                .idempotencyKey("k1")
                .build();

        when(transactionService.transfer(any(TransferDTO.class))).thenReturn(tx);

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(transactionService).transfer(any(TransferDTO.class));
    }

    @Test
    void transfer_InvalidBody_ReturnsBadRequest() throws Exception {
        TransferDTO dto = TransferDTO.builder()
                .fromCardId(null)
                .toCardId(2L)
                .amount(new BigDecimal("10.00"))
                .idempotencyKey("k1")
                .build();

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).transfer(any());
    }

    @Test
    void myTransactions_ReturnsOk() throws Exception {
        PageResponseDTO<TransactionDTO> page = PageResponseDTO.of(List.of(tx), 0, 10, 1);
        when(transactionService.myTransactions(any(), any())).thenReturn(page);

        mockMvc.perform(get("/transactions/me")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(transactionService).myTransactions(eq(0), eq(10));
    }

    @Test
    void myCardTransactions_ReturnsOk() throws Exception {
        PageResponseDTO<TransactionDTO> page = PageResponseDTO.of(List.of(tx), 0, 10, 1);
        when(transactionService.myCardTransactions(eq(1L), any(), any())).thenReturn(page);

        mockMvc.perform(get("/transactions/me/card/1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(transactionService).myCardTransactions(eq(1L), eq(0), eq(10));
    }
}
