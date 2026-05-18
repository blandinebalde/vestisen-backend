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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SellerPlanService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnonceRepository annonceRepository;

    @Autowired
    private SaleCommissionRepository saleCommissionRepository;

    @Autowired
    private SellerSubscriptionService sellerSubscriptionService;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    public List<SellerPlanCatalogItemDTO> getCatalog() {
        return SellerPlanCatalog.all().stream().map(this::toCatalogItem).collect(Collectors.toList());
    }

    public SellerSubscriptionStatusDTO getSubscriptionStatus(User user) {
        return sellerSubscriptionService.buildStatusDto(user);
    }

    public CommissionBreakdownDTO previewCommission(BigDecimal saleAmount, User seller) {
        User u = userRepository.findById(seller.getId()).orElseThrow();
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(u);
        SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
        return computeBreakdown(saleAmount, def);
    }

    public void assertCanPublish(User seller) {
        User u = userRepository.findByIdForUpdate(seller.getId()).orElseThrow();
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(u);
        SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
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
        User seller = userRepository.findById(annonce.getSeller().getId()).orElseThrow();
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(seller);
        BigDecimal saleAmount = annonce.getPrice() != null ? annonce.getPrice() : BigDecimal.ZERO;
        SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
        CommissionBreakdownDTO breakdown = computeBreakdown(saleAmount, def);

        SaleCommission sc = new SaleCommission();
        sc.setAnnonce(annonce);
        sc.setSeller(seller);
        sc.setBuyer(buyer);
        sc.setSaleAmountFcfa(breakdown.getSaleAmountFcfa());
        sc.setCommissionPercent(breakdown.getCommissionPercent());
        sc.setCommissionAmountFcfa(breakdown.getCommissionAmountFcfa());
        sc.setSellerNetFcfa(breakdown.getSellerNetFcfa());
        sc.setSellerPlanAtSale(sub.getPlanType());
        sc.setCreatedAt(LocalDateTime.now());
        return saleCommissionRepository.save(sc);
    }

    /**
     * Changement de plan : downgrade planifié ; upgrade via /checkout + confirmation (webhook ou démo).
     */
    public SellerSubscriptionStatusDTO subscribe(User seller, SellerPlan targetPlan, PlanBillingCycle cycle) {
        if (seller.getRole() != User.Role.VENDEUR && seller.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux vendeurs");
        }
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(seller);
        SellerPlan current = sub.getPlanType();
        if (targetPlan == current) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce plan est déjà actif.");
        }
        if (SubscriptionProrationService.planRank(targetPlan) < SubscriptionProrationService.planRank(current)) {
            return sellerSubscriptionService.scheduleDowngrade(seller, targetPlan, sub.getVersion());
        }
        if (isStripeConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Utilisez POST /api/seller/plan/checkout pour souscrire ou upgrader avec paiement sécurisé.");
        }
        var quote = sellerSubscriptionService.quote(seller, targetPlan, cycle);
        sellerSubscriptionService.applyPaidUpgrade(
                seller,
                targetPlan,
                cycle != null ? cycle : PlanBillingCycle.MONTHLY,
                quote.getAmountDueCents(),
                "demo-" + UUID.randomUUID(),
                null,
                null,
                SubscriptionActorType.SELLER,
                seller.getId());
        return sellerSubscriptionService.buildStatusDto(seller);
    }

    public Page<SaleCommissionDTO> listCommissionsForSeller(User seller, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        return saleCommissionRepository.findBySeller_IdOrderByCreatedAtDesc(seller.getId(), pageable)
                .map(this::toCommissionDto);
    }

    public void setPlanByAdmin(User seller, SellerPlan plan, PlanBillingCycle cycle) {
        User u = userRepository.findByIdForUpdate(seller.getId()).orElseThrow();
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(u);
        if (plan == SellerPlan.FREE) {
            sellerSubscriptionService.forceDowngradeToFree(sub, "Modification admin");
            return;
        }
        sellerSubscriptionService.applyPaidUpgrade(
                u, plan, cycle != null ? cycle : PlanBillingCycle.MONTHLY, 0L,
                "admin-" + UUID.randomUUID(), null, null,
                SubscriptionActorType.ADMIN, null);
    }

    public static final int MAX_ANNONCE_LIFETIME_DAYS = 365;

    public boolean isSubscriptionPeriodActive(User seller) {
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(seller);
        return sellerSubscriptionService.isSubscriptionPeriodActive(sub);
    }

    public boolean canPayWithSubscription(User seller) {
        if (!isSubscriptionPeriodActive(seller)) {
            return false;
        }
        SellerSubscription sub = sellerSubscriptionService.getOrCreate(seller);
        SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
        if (def.isUnlimitedPublications()) {
            return true;
        }
        long active = annonceRepository.countActivePublicationsBySeller(seller.getId());
        return active < def.getMaxActivePublications();
    }

    public void applySubscriptionPublication(User seller, PublicationTarif tarif) {
        if (!canPayWithSubscription(seller)) {
            if (!isSubscriptionPeriodActive(seller)) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Votre abonnement n'est plus actif. Renouvelez-le ou payez en crédits.");
            }
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Quota de publications actives atteint pour votre plan. Passez à un plan supérieur ou payez en crédits.");
        }
        if (tarif != null && tarif.isTopPublication()) {
            User u = userRepository.findByIdForUpdate(seller.getId()).orElseThrow();
            SellerSubscription sub = sellerSubscriptionService.getOrCreate(u);
            if (sub.getBoostsRemaining() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Aucun boost disponible sur votre plan pour une top publication. Payez en crédits ou attendez le prochain cycle.");
            }
            sub.setBoostsRemaining(sub.getBoostsRemaining() - 1);
            sellerSubscriptionService.syncUserFromSubscription(u, sub);
        }
    }

    @Scheduled(cron = "0 15 * * * ?")
    public void downgradeExpiredGracePlans() {
        sellerSubscriptionService.enforceGraceExpiry();
    }

    private CommissionBreakdownDTO computeBreakdown(BigDecimal saleAmount, SellerPlanDefinition def) {
        BigDecimal amount = saleAmount != null ? saleAmount.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal pct = def.getCommissionPercent();
        BigDecimal commission = amount.multiply(pct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal net = amount.subtract(commission).max(BigDecimal.ZERO);
        return new CommissionBreakdownDTO(amount, pct, commission, net, def.getPlan().name());
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
