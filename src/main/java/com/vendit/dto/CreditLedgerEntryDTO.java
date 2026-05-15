package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditLedgerEntryDTO {
    private UUID publicId;
    private String movementType;
    private BigDecimal amountDelta;
    private BigDecimal balanceAfter;
    private UUID annoncePublicId;
    private String referenceCode;
    private String creditTransactionCode;
    private UUID creditTransactionPublicId;
    private LocalDateTime createdAt;
}
