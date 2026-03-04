package com.example.bankcards.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserDTO user;
}
