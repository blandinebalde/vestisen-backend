package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerPlanCatalogItemDTO {
    private String plan;
    private String label;
    private BigDecimal monthlyPriceFcfa;
    private BigDecimal annualPriceFcfa;
    private BigDecimal commissionPercent;
    private int maxActivePublications;
    private boolean unlimitedPublications;
    private int monthlyBoostsIncluded;
}
