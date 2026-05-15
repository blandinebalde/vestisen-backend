package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsCreditsByMonthDTO {
    private int year;
    private int month;
    private String label; // "2024-01" ou "Janv. 2024"
    private BigDecimal creditsPurchased;
    private BigDecimal creditsSpent;
}
