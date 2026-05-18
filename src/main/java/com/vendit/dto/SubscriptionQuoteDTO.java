package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionQuoteDTO {
    private String targetPlan;
    private String targetPlanLabel;
    private String billingCycle;
    /** Montant immédiat en FCFA (entier affiché). */
    private long amountDueFcfa;
    /** Montant en centimes (source de vérité). */
    private long amountDueCents;
    private boolean upgrade;
    private boolean prorated;
    private Integer daysRemainingInCycle;
    private LocalDateTime renewalDate;
    private String downgradePolicyMessage;
    private boolean requires3ds;
    private boolean immediateActivation;
}
