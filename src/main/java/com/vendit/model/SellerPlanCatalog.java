package com.vendit.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SellerPlanCatalog {

    private static final Map<SellerPlan, SellerPlanDefinition> BY_PLAN = Arrays.stream(new SellerPlanDefinition[]{
            new SellerPlanDefinition(SellerPlan.FREE, "Gratuit", BigDecimal.ZERO, new BigDecimal("15"), 5, 0),
            new SellerPlanDefinition(SellerPlan.PRO, "Pro", new BigDecimal("2900"), new BigDecimal("8"), 50, 3),
            new SellerPlanDefinition(SellerPlan.PREMIUM, "Premium", new BigDecimal("9900"), new BigDecimal("5"), -1, 10)
    }).collect(Collectors.toMap(SellerPlanDefinition::getPlan, d -> d));

    private SellerPlanCatalog() {
    }

    public static SellerPlanDefinition get(SellerPlan plan) {
        SellerPlanDefinition def = BY_PLAN.get(plan != null ? plan : SellerPlan.FREE);
        return def != null ? def : BY_PLAN.get(SellerPlan.FREE);
    }

    public static List<SellerPlanDefinition> all() {
        return List.of(
                BY_PLAN.get(SellerPlan.FREE),
                BY_PLAN.get(SellerPlan.PRO),
                BY_PLAN.get(SellerPlan.PREMIUM));
    }
}
