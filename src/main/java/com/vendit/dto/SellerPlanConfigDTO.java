package com.vendit.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellerPlanConfigDTO {
    private String plan;
    private String label;
    private BigDecimal monthlyPriceFcfa;
    private BigDecimal annualPriceFcfa;
    private BigDecimal commissionPercent;
    private int maxActivePublications;
    private boolean unlimitedPublications;
    private int monthlyBoostsIncluded;
    private boolean active;
    private int displayOrder;
}
