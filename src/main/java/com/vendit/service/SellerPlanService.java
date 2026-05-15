package com.vendit.service;

import com.vendit.dto.CommissionBreakdownDTO;
import com.vendit.dto.SaleCommissionDTO;
import com.vendit.dto.SellerPlanCatalogItemDTO;
import com.vendit.dto.SellerSubscriptionStatusDTO;
import com.vendit.model.*;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.SaleCommissionRepository;
import com.vendit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SellerPlanService {

    private static final int GRACE_DAYS = 7;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnonceRepository annonceRepository;

    @Autowired
    private SaleCommissionRepository saleCommissionRepository;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    public List<SellerPlanCatalogItemDTO> getCatalog() {
        return SellerPlanCatalog.all().stream().map(this::toCatalogItem).collect(Collectors.toList());
    }

    public SellerSubscriptionStatusDTO getSubscriptionStatus(User user) {
        User u = refreshPlanState(userRepository.findById(user.getId()).orElseThrow());
        return buildStatus(u);
    }

    public CommissionBreakdownDTO previewCommission(BigDecimal saleAmount, User seller) {
        User u = refreshPlanState(userRepository.findById(seller.getId()).orElseThrow());
        return computeBreakdown(saleAmount, SellerPlanCatalog.get(u.getSellerPlan()));
    }

    public void assertCanPublish(User seller) {
        User u = refreshPlanState(userRepository.findByIdForUpdate(seller.getId()).orElseThrow());
        SellerPlanDefinition def = SellerPlanCatalog.get(u.getSellerPlan());
        if (def.isUnlimitedPublications()) {
            return;
        }
        long active = annonceRepository.countActivePublicationsBySeller(u.getId());
        if (active >= def.getMaxActivePublications()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Limite du plan " + def.getLabel() + " atteinte ("
                            + def.getMaxActivePublications()
                            + " publications actives). Passez au plan Pro ou Premium, ou désactivez des annonces.");
        }
    }

    public SaleCommission recordSaleCommission(Annonce annonce, User buyer) {
        if (saleCommissionRepository.existsByAnnonce_Id(annonce.getId())) {
            return saleCommissionRepository.findByAnnonce_Id(annonce.getId()).orElse(null);
        }
        User seller = refreshPlanState(userRepository.findById(annonce.getSeller().getId()).orElseThrow());
        BigDecimal saleAmount = annonce.getPrice() != null ? annonce.getPrice() : BigDecimal.ZERO;
        SellerPlanDefinition def = SellerPlanCatalog.get(seller.getSellerPlan());
        CommissionBreakdownDTO breakdown = computeBreakdown(saleAmount, def);

        SaleCommission sc = new SaleCommission();
        sc.setAnnonce(annonce);
        sc.setSeller(seller);
        sc.setBuyer(buyer);
        sc.setSaleAmountFcfa(breakdown.getSaleAmountFcfa());
        sc.setCommissionPercent(breakdown.getCommissionPercent());
        sc.setCommissionAmountFcfa(breakdown.getCommissionAmountFcfa());
        sc.setSellerNetFcfa(breakdown.getSellerNetFcfa());
        sc.setSellerPlanAtSale(seller.getSellerPlan());
        sc.setCreatedAt(LocalDateTime.now());
        return saleCommissionRepository.save(sc);
    }

    /**
     * Souscription au plan. Stripe Billing à brancher : si clé Stripe absente, activation locale (dev / démo).
     */
    public SellerSubscriptionStatusDTO subscribe(User seller, SellerPlan targetPlan, PlanBillingCycle cycle) {
        if (seller.getRole() != User.Role.VENDEUR && seller.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux vendeurs");
        }
        User u = userRepository.findByIdForUpdate(seller.getId()).orElseThrow();
        refreshPlanState(u);

        if (targetPlan == SellerPlan.FREE) {
            applyPlan(u, SellerPlan.FREE, null, false);
            return buildStatus(userRepository.save(u));
        }

        if (!isStripeConfigured()) {
            applyPlan(u, targetPlan, cycle != null ? cycle : PlanBillingCycle.MONTHLY, true);
            return buildStatus(userRepository.save(u));
        }

        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Paiement abonnement Stripe Billing à configurer. Utilisez la clé Stripe ou le mode démo sans clé.");
    }

    public Page<SaleCommissionDTO> listCommissionsForSeller(User seller, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        return saleCommissionRepository.findBySeller_IdOrderByCreatedAtDesc(seller.getId(), pageable)
                .map(this::toCommissionDto);
    }

    public void setPlanByAdmin(User seller, SellerPlan plan, PlanBillingCycle cycle) {
        User u = userRepository.findByIdForUpdate(seller.getId()).orElseThrow();
        applyPlan(u, plan, cycle, plan != SellerPlan.FREE);
        userRepository.save(u);
    }

    @Scheduled(cron = "0 15 * * * ?")
    public void downgradeExpiredGracePlans() {
        LocalDateTime now = LocalDateTime.now();
        List<User> vendeurs = userRepository.findByRoleAndPlanGraceUntilBefore(User.Role.VENDEUR, now);
        for (User u : vendeurs) {
            if (u.getSellerPlan() == SellerPlan.FREE) {
                continue;
            }
            applyPlan(u, SellerPlan.FREE, null, false);
            userRepository.save(u);
        }
    }

    private User refreshPlanState(User u) {
        if (u == null) {
            return null;
        }
        if (u.getSellerPlan() == null) {
            u.setSellerPlan(SellerPlan.FREE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (u.getPlanPeriodEnd() != null && now.isAfter(u.getPlanPeriodEnd())
                && (u.getPlanGraceUntil() == null || now.isAfter(u.getPlanGraceUntil()))
                && u.getSellerPlan() != SellerPlan.FREE) {
            applyPlan(u, SellerPlan.FREE, null, false);
            userRepository.save(u);
        } else {
            syncBoostsForCurrentPeriod(u);
        }
        return u;
    }

    private void applyPlan(User u, SellerPlan plan, PlanBillingCycle cycle, boolean paidActive) {
        SellerPlanDefinition def = SellerPlanCatalog.get(plan);
        u.setSellerPlan(plan);
        u.setPlanBillingCycle(plan == SellerPlan.FREE ? null : cycle);
        LocalDateTime now = LocalDateTime.now();
        if (paidActive && plan != SellerPlan.FREE) {
            u.setPlanPeriodStart(now);
            int months = cycle == PlanBillingCycle.ANNUAL ? 12 : 1;
            u.setPlanPeriodEnd(now.plusMonths(months));
            u.setPlanGraceUntil(null);
            u.setBoostsPeriodStart(now);
            u.setBoostsRemaining(def.getMonthlyBoostsIncluded());
        } else {
            u.setPlanPeriodStart(null);
            u.setPlanPeriodEnd(null);
            u.setPlanGraceUntil(null);
            u.setStripeSubscriptionId(null);
            u.setBoostsPeriodStart(null);
            u.setBoostsRemaining(0);
        }
    }

    private void syncBoostsForCurrentPeriod(User u) {
        if (u.getSellerPlan() == SellerPlan.FREE) {
            return;
        }
        SellerPlanDefinition def = SellerPlanCatalog.get(u.getSellerPlan());
        LocalDateTime periodStart = u.getBoostsPeriodStart();
        LocalDateTime planStart = u.getPlanPeriodStart();
        if (planStart != null && (periodStart == null || periodStart.isBefore(planStart))) {
            u.setBoostsPeriodStart(planStart);
            u.setBoostsRemaining(def.getMonthlyBoostsIncluded());
        }
    }

    private CommissionBreakdownDTO computeBreakdown(BigDecimal saleAmount, SellerPlanDefinition def) {
        BigDecimal amount = saleAmount != null ? saleAmount.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal pct = def.getCommissionPercent();
        BigDecimal commission = amount.multiply(pct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal net = amount.subtract(commission).max(BigDecimal.ZERO);
        return new CommissionBreakdownDTO(amount, pct, commission, net, def.getPlan().name());
    }

    private SellerSubscriptionStatusDTO buildStatus(User u) {
        SellerPlanDefinition def = SellerPlanCatalog.get(u.getSellerPlan());
        long active = annonceRepository.countActivePublicationsBySeller(u.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean grace = u.getPlanGraceUntil() != null && !now.isAfter(u.getPlanGraceUntil());
        return new SellerSubscriptionStatusDTO(
                u.getSellerPlan().name(),
                def.getLabel(),
                def.getCommissionPercent(),
                def.getMaxActivePublications(),
                def.isUnlimitedPublications(),
                active,
                u.getBoostsRemaining(),
                def.getMonthlyBoostsIncluded(),
                u.getPlanBillingCycle() != null ? u.getPlanBillingCycle().name() : null,
                u.getPlanPeriodStart(),
                u.getPlanPeriodEnd(),
                u.getPlanGraceUntil(),
                grace,
                u.getCreditBalance() != null ? u.getCreditBalance() : BigDecimal.ZERO);
    }

    private SellerPlanCatalogItemDTO toCatalogItem(SellerPlanDefinition d) {
        return new SellerPlanCatalogItemDTO(
                d.getPlan().name(),
                d.getLabel(),
                d.getMonthlyPriceFcfa(),
                d.annualPriceFcfa().setScale(0, RoundingMode.HALF_UP),
                d.getCommissionPercent(),
                d.getMaxActivePublications(),
                d.isUnlimitedPublications(),
                d.getMonthlyBoostsIncluded());
    }

    private SaleCommissionDTO toCommissionDto(SaleCommission sc) {
        return new SaleCommissionDTO(
                sc.getPublicId(),
                sc.getAnnonce().getPublicId(),
                sc.getAnnonce().getTitle(),
                sc.getSaleAmountFcfa(),
                sc.getCommissionPercent(),
                sc.getCommissionAmountFcfa(),
                sc.getSellerNetFcfa(),
                sc.getSellerPlanAtSale().name(),
                sc.getCreatedAt());
    }

    private boolean isStripeConfigured() {
        return stripeSecretKey != null
                && !stripeSecretKey.isBlank()
                && !stripeSecretKey.contains("your_stripe");
    }
}
