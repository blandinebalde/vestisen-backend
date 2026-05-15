package com.vendit.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/** Catalogue des plans vendeur (tarifs et limites spec fonctionnelle). */
@Getter
@AllArgsConstructor
public class SellerPlanDefinition {
    private final SellerPlan plan;
    private final String label;
    private final BigDecimal monthlyPriceFcfa;
    private final BigDecimal commissionPercent;
    /** -1 = publications actives illimitées */
    private final int maxActivePublications;
    private final int monthlyBoostsIncluded;

    public boolean isUnlimitedPublications() {
        return maxActivePublications < 0;
    }

    public BigDecimal annualPriceFcfa() {
        return monthlyPriceFcfa.multiply(new BigDecimal("12"))
                .multiply(new BigDecimal("0.80"));
    }
}
