package com.vendit.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class SellerPlanCatalog {

    private static final Map<SellerPlan, SellerPlanDefinition> STATIC_BY_PLAN = Arrays.stream(new SellerPlanDefinition[]{
            new SellerPlanDefinition(SellerPlan.FREE, "Gratuit", BigDecimal.ZERO, new BigDecimal("15"), 5, 0),
            new SellerPlanDefinition(SellerPlan.PRO, "Pro", new BigDecimal("2900"), new BigDecimal("8"), 50, 3),
            new SellerPlanDefinition(SellerPlan.PREMIUM, "Premium", new BigDecimal("9900"), new BigDecimal("5"), -1, 10)
    }).collect(Collectors.toMap(SellerPlanDefinition::getPlan, d -> d));

    private static volatile Supplier<List<SellerPlanDefinition>> dbLoader;

    private SellerPlanCatalog() {
    }

    public static void setLoader(Supplier<List<SellerPlanDefinition>> loader) {
        dbLoader = loader;
    }

    public static SellerPlanDefinition get(SellerPlan plan) {
        SellerPlan p = plan != null ? plan : SellerPlan.FREE;
        if (dbLoader != null) {
            List<SellerPlanDefinition> fromDb = dbLoader.get();
            if (fromDb != null && !fromDb.isEmpty()) {
                return fromDb.stream()
                        .filter(d -> d.getPlan() == p)
                        .findFirst()
                        .orElse(fromDb.get(0));
            }
        }
        SellerPlanDefinition def = STATIC_BY_PLAN.get(p);
        return def != null ? def : STATIC_BY_PLAN.get(SellerPlan.FREE);
    }

    public static List<SellerPlanDefinition> all() {
        if (dbLoader != null) {
            List<SellerPlanDefinition> fromDb = dbLoader.get();
            if (fromDb != null && !fromDb.isEmpty()) {
                return List.copyOf(fromDb);
            }
        }
        return List.of(
                STATIC_BY_PLAN.get(SellerPlan.FREE),
                STATIC_BY_PLAN.get(SellerPlan.PRO),
                STATIC_BY_PLAN.get(SellerPlan.PREMIUM));
    }
}
