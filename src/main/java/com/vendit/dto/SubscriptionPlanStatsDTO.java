package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanStatsDTO {
    private long totalVendeurs;
    private long paidSubscribers;
    private long pastDueCount;
    private long scheduledDowngrades;
    private long publishedPlansCount;
    private long estimatedMrrFcfa;
    private List<PlanSubscriberCountDTO> byPlan;
    private List<StatusCountDTO> byStatus;
}
