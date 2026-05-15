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
public class SaleCommissionDTO {
    private UUID publicId;
    private UUID annoncePublicId;
    private String annonceTitle;
    private BigDecimal saleAmountFcfa;
    private BigDecimal commissionPercent;
    private BigDecimal commissionAmountFcfa;
    private BigDecimal sellerNetFcfa;
    private String sellerPlanAtSale;
    private LocalDateTime createdAt;
}
