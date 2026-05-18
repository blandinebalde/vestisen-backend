package com.vendit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_plan_configs")
@Data
public class SellerPlanConfig {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", length = 20)
    private SellerPlan planCode;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(name = "monthly_price_fcfa", nullable = false)
    private BigDecimal monthlyPriceFcfa = BigDecimal.ZERO;

    @Column(name = "commission_percent", nullable = false)
    private BigDecimal commissionPercent = new BigDecimal("15");

    /** -1 = illimité */
    @Column(name = "max_active_publications", nullable = false)
    private int maxActivePublications = 5;

    @Column(name = "monthly_boosts_included", nullable = false)
    private int monthlyBoostsIncluded = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SellerPlanDefinition toDefinition() {
        return new SellerPlanDefinition(
                planCode,
                label,
                monthlyPriceFcfa,
                commissionPercent,
                maxActivePublications,
                monthlyBoostsIncluded);
    }
}
