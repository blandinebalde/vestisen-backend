package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsCreditsByUserDTO {
    private Long userId;
    private String userEmail;
    private BigDecimal creditsPurchased;
}
