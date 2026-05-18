package com.vendit.service;

import com.vendit.dto.PlanSubscriberCountDTO;
import com.vendit.dto.StatusCountDTO;
import com.vendit.dto.SubscriptionPlanStatsDTO;
import com.vendit.model.SellerPlan;
import com.vendit.model.SellerPlanCatalog;
import com.vendit.model.SellerPlanDefinition;
import com.vendit.model.SellerSubscription;
import com.vendit.model.SubscriptionStatus;
import com.vendit.repository.SellerPlanConfigRepository;
import com.vendit.repository.SellerSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminSubscriptionStatsService {

    @Autowired
    private SellerSubscriptionRepository subscriptionRepository;

    @Autowired
    private SellerPlanConfigRepository planConfigRepository;

    public SubscriptionPlanStatsDTO getStats() {
        Map<SellerPlan, Long> countByPlan = new EnumMap<>(SellerPlan.class);
        for (SellerPlan p : SellerPlan.values()) {
            countByPlan.put(p, 0L);
        }
        List<Object[]> planRows = subscriptionRepository.countVendeurSubscriptionsByPlan();
        long total = 0;
        for (Object[] row : planRows) {
            SellerPlan plan = (SellerPlan) row[0];
            long c = row[1] != null ? ((Number) row[1]).longValue() : 0;
            countByPlan.put(plan, c);
            total += c;
        }

        List<PlanSubscriberCountDTO> byPlan = new ArrayList<>();
        for (SellerPlan plan : List.of(SellerPlan.FREE, SellerPlan.PRO, SellerPlan.PREMIUM)) {
            SellerPlanDefinition def = SellerPlanCatalog.get(plan);
            byPlan.add(new PlanSubscriberCountDTO(
                    plan.name(),
                    def.getLabel(),
                    countByPlan.getOrDefault(plan, 0L)));
        }

        List<StatusCountDTO> byStatus = new ArrayList<>();
        for (Object[] row : subscriptionRepository.countVendeurSubscriptionsByStatus()) {
            SubscriptionStatus st = (SubscriptionStatus) row[0];
            long c = row[1] != null ? ((Number) row[1]).longValue() : 0;
            byStatus.add(new StatusCountDTO(st.name(), statusLabel(st), c));
        }

        long mrr = 0;
        List<SellerSubscription> paidActive = subscriptionRepository.findAll().stream()
                .filter(s -> s.getUser() != null && s.getUser().getRole() == com.vendit.model.User.Role.VENDEUR)
                .filter(s -> s.getPlanType() == SellerPlan.PRO || s.getPlanType() == SellerPlan.PREMIUM)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE || s.getStatus() == SubscriptionStatus.PAST_DUE)
                .toList();
        for (SellerSubscription sub : paidActive) {
            SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
            mrr += def.getMonthlyPriceFcfa().longValue();
        }

        long publishedPlans = planConfigRepository.findAll().stream().filter(c -> c.isActive()).count();
        if (publishedPlans == 0) {
            publishedPlans = 3;
        }

        return new SubscriptionPlanStatsDTO(
                total,
                subscriptionRepository.countPaidSubscribers(),
                subscriptionRepository.countVendeurByStatus(SubscriptionStatus.PAST_DUE),
                subscriptionRepository.countByScheduledDowngradeIsNotNull(),
                publishedPlans,
                mrr,
                byPlan,
                byStatus);
    }

    private static String statusLabel(SubscriptionStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case ACTIVE -> "Actif";
            case PAST_DUE -> "Paiement en retard";
            case CANCELLED -> "Annulé";
            case TRIALING -> "Essai";
        };
    }
}
