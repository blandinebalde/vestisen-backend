package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerSubscriptionStatusDTO {
    private String currentPlan;
    private String planLabel;
    private BigDecimal commissionPercent;
    private int maxActivePublications;
    private boolean unlimitedPublications;
    private long activePublicationsCount;
    private int boostsRemaining;
    private int monthlyBoostsIncluded;
    private String billingCycle;
    private LocalDateTime planPeriodStart;
    private LocalDateTime planPeriodEnd;
    private LocalDateTime planGraceUntil;
    private boolean inGracePeriod;
    private BigDecimal creditBalance;
    /** Abonnement encore valide (période payante ou Gratuit). */
    private boolean subscriptionPeriodActive;
    /** Peut publier en utilisant le quota abonnement (sans crédits). */
    private boolean canPayWithSubscription;
    /** ACTIVE, PAST_DUE, CANCELLED, TRIALING */
    private String subscriptionStatus;
    private String scheduledDowngrade;
    private String scheduledDowngradeLabel;
    private boolean downgradeLocked;
    private Long subscriptionVersion;
}
