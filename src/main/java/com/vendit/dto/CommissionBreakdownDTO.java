package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommissionBreakdownDTO {
    private BigDecimal saleAmountFcfa;
    private BigDecimal commissionPercent;
    private BigDecimal commissionAmountFcfa;
    private BigDecimal sellerNetFcfa;
    private String sellerPlan;
}
