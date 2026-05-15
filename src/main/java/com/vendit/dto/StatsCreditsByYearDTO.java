package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsCreditsByYearDTO {
    private int year;
    private BigDecimal creditsPurchased;
    private BigDecimal creditsSpent;
}
