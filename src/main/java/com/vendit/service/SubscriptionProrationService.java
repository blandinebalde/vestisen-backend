package com.vendit.service;

import com.vendit.model.PlanBillingCycle;
import com.vendit.model.SellerPlan;
import com.vendit.model.SellerPlanDefinition;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SubscriptionProrationService {

    private static final int DEFAULT_CYCLE_DAYS = 30;
    private static final long THREE_DS_THRESHOLD_CENTS = 500_000L; // 5 000 FCFA

    public long computeUpgradeAmountCents(
            SellerPlan currentPlan,
            SellerPlan targetPlan,
            PlanBillingCycle billingCycle,
            SellerPlanDefinition currentDef,
            SellerPlanDefinition targetDef,
            LocalDateTime startDate,
            LocalDateTime renewalDate) {
        if (planRank(targetPlan) <= planRank(currentPlan)) {
            return 0L;
        }
        BigDecimal targetPrice = cyclePrice(targetDef, billingCycle);
        if (currentPlan == SellerPlan.FREE || startDate == null || renewalDate == null) {
            return toCents(targetPrice);
        }
        LocalDateTime now = LocalDateTime.now();
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(startDate.toLocalDate(), renewalDate.toLocalDate()));
        long daysRemaining = ChronoUnit.DAYS.between(now.toLocalDate(), renewalDate.toLocalDate());
        if (daysRemaining < 0) {
            daysRemaining = 0;
        }
        BigDecimal currentPrice = cyclePrice(currentDef, billingCycle);
        BigDecimal diff = targetPrice.subtract(currentPrice);
        if (diff.signum() <= 0) {
            return 0L;
        }
        BigDecimal prorated = diff
                .multiply(BigDecimal.valueOf(daysRemaining))
                .divide(BigDecimal.valueOf(totalDays), 0, RoundingMode.HALF_UP);
        return toCents(prorated);
    }

    public long computeFullCycleAmountCents(SellerPlanDefinition def, PlanBillingCycle cycle) {
        return toCents(cyclePrice(def, cycle));
    }

    public boolean requires3ds(long amountCents) {
        return amountCents >= THREE_DS_THRESHOLD_CENTS;
    }

    public static int planRank(SellerPlan plan) {
        if (plan == null || plan == SellerPlan.FREE) {
            return 0;
        }
        if (plan == SellerPlan.PRO) {
            return 1;
        }
        return 2;
    }

    private static BigDecimal cyclePrice(SellerPlanDefinition def, PlanBillingCycle cycle) {
        if (cycle == PlanBillingCycle.ANNUAL) {
            return def.annualPriceFcfa().setScale(0, RoundingMode.HALF_UP);
        }
        return def.getMonthlyPriceFcfa().setScale(0, RoundingMode.HALF_UP);
    }

    private static long toCents(BigDecimal fcfa) {
        return fcfa.multiply(BigDecimal.valueOf(100)).longValue();
    }

    public static long centsToFcfa(long cents) {
        return cents / 100;
    }
}
