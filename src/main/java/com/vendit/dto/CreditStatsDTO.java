package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditStatsDTO {
    /** Total des crédits achetés (transactions complétées). */
    private BigDecimal creditsPurchased;
    /** Total des crédits dépensés (publications d'annonces). */
    private BigDecimal creditsSpent;
}
