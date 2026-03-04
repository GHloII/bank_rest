package com.example.bankcards.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCardDTO {
    
    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "\\d{16}", message = "PAN must be exactly 16 digits")
    private String pan;
    
    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    private String ownerName;
    
    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;
    
    @NotNull(message = "Expiry year is required")
    @Min(value = 2000, message = "Expiry year must be 2000 or later")
    private Integer expiryYear;
}
