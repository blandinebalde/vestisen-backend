package com.vendit.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.vendit.dto.SellerSubscriptionStatusDTO;
import com.vendit.dto.SubscriptionCheckoutDTO;
import com.vendit.dto.SubscriptionQuoteDTO;
import com.vendit.model.*;
import com.vendit.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class SellerSubscriptionService {

    private static final int GRACE_DAYS = 7;
    private static final int CHECKOUT_TTL_HOURS = 2;

    @Autowired
    private SellerSubscriptionRepository subscriptionRepository;
    @Autowired
    private SubscriptionAuditLogRepository auditLogRepository;
    @Autowired
    private SubscriptionFinancialTransactionRepository financialRepository;
    @Autowired
    private SubscriptionPendingCheckoutRepository pendingCheckoutRepository;
    @Autowired
    private StripeWebhookEventRepository webhookEventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnnonceRepository annonceRepository;
    @Autowired
    private SubscriptionProrationService prorationService;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    public SellerSubscription getOrCreate(User user) {
        return subscriptionRepository.findByUser_Id(user.getId())
                .orElseGet(() -> createFromUser(user));
    }

    public SellerSubscriptionStatusDTO buildStatusDto(User user) {
        SellerSubscription sub = refreshState(getOrCreate(user));
        return toStatusDto(user, sub);
    }

    public SubscriptionQuoteDTO quote(User seller, SellerPlan targetPlan, PlanBillingCycle cycle) {
        assertVendor(seller);
        SellerSubscription sub = refreshState(getOrCreate(seller));
        SellerPlan current = sub.getPlanType();
        if (targetPlan == current) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce plan est déjà actif.");
        }
        if (SubscriptionProrationService.planRank(targetPlan) < SubscriptionProrationService.planRank(current)) {
            if (sub.isDowngradeLocked()) {
                String msg = sub.getRenewalDate() != null
                        ? "Le downgrade n'est possible qu'à partir du "
                                + sub.getRenewalDate().format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
                        : "Le downgrade n'est pas immédiat pendant votre cycle en cours.";
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
            }
        }
        SellerPlanDefinition currentDef = SellerPlanCatalog.get(current);
        SellerPlanDefinition targetDef = SellerPlanCatalog.get(targetPlan);
        PlanBillingCycle billing = cycle != null ? cycle : PlanBillingCycle.MONTHLY;
        boolean upgrade = SubscriptionProrationService.planRank(targetPlan) > SubscriptionProrationService.planRank(current);
        long amountCents;
        boolean prorated = false;
        Integer daysRemaining = null;
        if (upgrade && current != SellerPlan.FREE && sub.getStartDate() != null && sub.getRenewalDate() != null) {
            amountCents = prorationService.computeUpgradeAmountCents(
                    current, targetPlan, billing, currentDef, targetDef, sub.getStartDate(), sub.getRenewalDate());
            prorated = amountCents > 0 && amountCents < prorationService.computeFullCycleAmountCents(targetDef, billing);
            daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDateTime.now().toLocalDate(), sub.getRenewalDate().toLocalDate());
        } else if (upgrade) {
            amountCents = prorationService.computeFullCycleAmountCents(targetDef, billing);
        } else {
            amountCents = 0L;
        }
        SubscriptionQuoteDTO dto = new SubscriptionQuoteDTO();
        dto.setTargetPlan(targetPlan.name());
        dto.setTargetPlanLabel(targetDef.getLabel());
        dto.setBillingCycle(billing.name());
        dto.setAmountDueCents(amountCents);
        dto.setAmountDueFcfa(SubscriptionProrationService.centsToFcfa(amountCents));
        dto.setUpgrade(upgrade);
        dto.setProrated(prorated);
        dto.setDaysRemainingInCycle(daysRemaining);
        dto.setRenewalDate(sub.getRenewalDate());
        dto.setRequires3ds(prorationService.requires3ds(amountCents));
        dto.setImmediateActivation(false);
        dto.setDowngradePolicyMessage(
                "Un downgrade prend effet à la fin de votre cycle de facturation. Vous conservez tous vos avantages jusqu'à cette date.");
        return dto;
    }

    public SubscriptionCheckoutDTO startCheckout(
            User seller, SellerPlan targetPlan, PlanBillingCycle cycle, String idempotencyKey) {
        assertVendor(seller);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clé d'idempotence requise.");
        }
        if (pendingCheckoutRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande de paiement a déjà été initiée.");
        }
        SubscriptionQuoteDTO quote = quote(seller, targetPlan, cycle);
        if (!quote.isUpgrade()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Utilisez la planification de downgrade pour un plan inférieur.");
        }
        if (quote.getAmountDueCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Montant invalide pour ce changement de plan.");
        }
        SubscriptionPendingCheckout pending = new SubscriptionPendingCheckout();
        pending.setUserId(seller.getId());
        pending.setTargetPlan(targetPlan);
        pending.setBillingCycle(cycle != null ? cycle : PlanBillingCycle.MONTHLY);
        pending.setAmountCents(quote.getAmountDueCents());
        pending.setIdempotencyKey(idempotencyKey.trim());
        pending.setExpiresAt(LocalDateTime.now().plusHours(CHECKOUT_TTL_HOURS));
        String clientSecret = null;
        boolean stripeEnabled = isStripeConfigured();
        if (stripeEnabled) {
            try {
                Stripe.apiKey = stripeSecretKey.trim();
                PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                        .setAmount(quote.getAmountDueCents())
                        .setCurrency("xof")
                        .putMetadata("userId", String.valueOf(seller.getId()))
                        .putMetadata("targetPlan", targetPlan.name())
                        .putMetadata("billingCycle", pending.getBillingCycle().name())
                        .putMetadata("idempotencyKey", idempotencyKey)
                        .putMetadata("checkoutId", pending.getCheckoutId());
                if (quote.isRequires3ds()) {
                    params.setPaymentMethodOptions(
                            PaymentIntentCreateParams.PaymentMethodOptions.builder()
                                    .setCard(PaymentIntentCreateParams.PaymentMethodOptions.Card.builder()
                                            .setRequestThreeDSecure(
                                                    PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.ANY)
                                            .build())
                                    .build());
                }
                PaymentIntent intent = PaymentIntent.create(params.build());
                pending.setStripePaymentIntentId(intent.getId());
                clientSecret = intent.getClientSecret();
            } catch (StripeException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Impossible d'initialiser le paiement Stripe : " + e.getMessage());
            }
        }
        pendingCheckoutRepository.save(pending);
        return new SubscriptionCheckoutDTO(
                pending.getCheckoutId(),
                clientSecret,
                quote.getAmountDueFcfa(),
                quote.getAmountDueCents(),
                stripeEnabled,
                !stripeEnabled,
                stripeEnabled
                        ? "Finalisez le paiement via Stripe. L'activation intervient après confirmation."
                        : "Mode démo : confirmez le paiement pour activer le plan (équivalent webhook invoice.paid).");
    }

    /** Confirmation démo ou complément webhook — active le plan uniquement après paiement validé. */
    public SellerSubscriptionStatusDTO confirmCheckout(User seller, String checkoutId, String idempotencyKey) {
        assertVendor(seller);
        SubscriptionPendingCheckout pending = pendingCheckoutRepository.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session de paiement introuvable."));
        if (!pending.getUserId().equals(seller.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Session de paiement expirée.");
        }
        if (!pending.getIdempotencyKey().equals(idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clé d'idempotence invalide.");
        }
        applyPaidUpgrade(
                seller,
                pending.getTargetPlan(),
                pending.getBillingCycle(),
                pending.getAmountCents(),
                pending.getIdempotencyKey(),
                pending.getStripePaymentIntentId(),
                null,
                SubscriptionActorType.SELLER,
                seller.getId());
        pendingCheckoutRepository.delete(pending);
        return buildStatusDto(seller);
    }

    public SellerSubscriptionStatusDTO scheduleDowngrade(User seller, SellerPlan targetPlan, Long expectedVersion) {
        assertVendor(seller);
        SellerSubscription sub = lockSubscription(seller.getId(), expectedVersion);
        if (SubscriptionProrationService.planRank(targetPlan) >= SubscriptionProrationService.planRank(sub.getPlanType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce n'est pas un downgrade.");
        }
        if (sub.getPlanType() == SellerPlan.FREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous êtes déjà sur le plan Gratuit.");
        }
        SellerPlan previous = sub.getPlanType();
        sub.setScheduledDowngrade(targetPlan);
        audit(sub, SubscriptionActorType.SELLER, seller.getId(), previous, targetPlan,
                sub.getStatus(), sub.getStatus(), "Downgrade planifié vers " + targetPlan);
        syncUserFromSubscription(sub.getUser(), sub);
        return toStatusDto(sub.getUser(), subscriptionRepository.save(sub));
    }

    public SellerSubscriptionStatusDTO cancelScheduledDowngrade(User seller, Long expectedVersion) {
        assertVendor(seller);
        SellerSubscription sub = lockSubscription(seller.getId(), expectedVersion);
        if (sub.getScheduledDowngrade() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun downgrade planifié.");
        }
        sub.setScheduledDowngrade(null);
        audit(sub, SubscriptionActorType.SELLER, seller.getId(), sub.getPlanType(), sub.getPlanType(),
                sub.getStatus(), sub.getStatus(), "Downgrade planifié annulé");
        syncUserFromSubscription(sub.getUser(), sub);
        return toStatusDto(sub.getUser(), subscriptionRepository.save(sub));
    }

    public void applyPaidUpgrade(
            User seller,
            SellerPlan targetPlan,
            PlanBillingCycle cycle,
            long amountCents,
            String idempotencyKey,
            String paymentIntentId,
            String invoiceId,
            SubscriptionActorType actor,
            Long actorUserId) {
        if (financialRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        SellerSubscription sub = subscriptionRepository.findByUserIdForUpdate(seller.getId())
                .orElseGet(() -> createFromUser(seller));
        SellerPlan previous = sub.getPlanType();
        SellerPlanDefinition def = SellerPlanCatalog.get(targetPlan);
        LocalDateTime now = LocalDateTime.now();
        boolean midCycleUpgrade = previous != SellerPlan.FREE && sub.getRenewalDate() != null && now.isBefore(sub.getRenewalDate());
        if (!midCycleUpgrade) {
            sub.setStartDate(now);
            int months = cycle == PlanBillingCycle.ANNUAL ? 12 : 1;
            sub.setRenewalDate(now.plusMonths(months));
        }
        sub.setPlanType(targetPlan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setBillingCycle(cycle);
        sub.setCommissionRate(def.getCommissionPercent());
        sub.setScheduledDowngrade(null);
        sub.setGraceUntil(null);
        sub.setDowngradeLocked(targetPlan != SellerPlan.FREE);
        int extraBoosts = def.getMonthlyBoostsIncluded();
        if (midCycleUpgrade) {
            sub.setBoostsRemaining(sub.getBoostsRemaining() + Math.max(0, extraBoosts - sub.getBoostsRemaining()));
        } else {
            sub.setBoostsRemaining(extraBoosts);
        }
        recordFinancial(sub, SubscriptionFinancialType.UPGRADE_PRORATA, amountCents, idempotencyKey, paymentIntentId, invoiceId);
        audit(sub, actor, actorUserId, previous, targetPlan, sub.getStatus(), SubscriptionStatus.ACTIVE,
                midCycleUpgrade ? "Upgrade immédiat (prorata)" : "Nouvelle souscription");
        syncUserFromSubscription(sub.getUser(), sub);
        subscriptionRepository.save(sub);
    }

    public boolean handleStripeWebhook(String payload, String signatureHeader) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            return false;
        }
        com.stripe.model.Event event;
        try {
            event = com.stripe.net.Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret.trim());
        } catch (Exception e) {
            return false;
        }
        if (webhookEventRepository.existsByStripeEventId(event.getId())) {
            return true;
        }
        StripeWebhookEvent record = new StripeWebhookEvent();
        record.setStripeEventId(event.getId());
        record.setEventType(event.getType());
        webhookEventRepository.save(record);
        switch (event.getType()) {
            case "invoice.paid" -> onInvoicePaid(event);
            case "invoice.payment_failed" -> onInvoicePaymentFailed(event);
            case "customer.subscription.deleted" -> onSubscriptionDeleted(event);
            case "payment_intent.succeeded" -> onPaymentIntentSucceeded(event);
            default -> { }
        }
        return true;
    }

    @Scheduled(cron = "0 30 * * * ?")
    public void enforceGraceExpiry() {
        LocalDateTime now = LocalDateTime.now();
        List<SellerSubscription> expired = subscriptionRepository.findByStatusAndGraceUntilBefore(
                SubscriptionStatus.PAST_DUE, now);
        for (SellerSubscription sub : expired) {
            forceDowngradeToFree(sub, "Fin du délai de grâce (7 jours)");
        }
    }

    public boolean isSubscriptionPeriodActive(SellerSubscription sub) {
        if (sub == null) {
            return true;
        }
        if (sub.getPlanType() == SellerPlan.FREE) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        if (sub.getRenewalDate() == null || !now.isAfter(sub.getRenewalDate())) {
            return sub.getStatus() == SubscriptionStatus.ACTIVE
                    || sub.getStatus() == SubscriptionStatus.TRIALING
                    || sub.getStatus() == SubscriptionStatus.PAST_DUE;
        }
        return sub.getGraceUntil() != null && !now.isAfter(sub.getGraceUntil());
    }

    public void syncUserFromSubscription(User user, SellerSubscription sub) {
        user.setSellerPlan(sub.getPlanType());
        user.setPlanBillingCycle(sub.getBillingCycle());
        user.setPlanPeriodStart(sub.getStartDate());
        user.setPlanPeriodEnd(sub.getRenewalDate());
        user.setPlanGraceUntil(sub.getGraceUntil());
        user.setStripeSubscriptionId(sub.getStripeSubscriptionId());
        user.setBoostsRemaining(sub.getBoostsRemaining());
        userRepository.save(user);
    }

    private void onInvoicePaid(com.stripe.model.Event event) {
        String subscriptionId = extractSubscriptionId(event);
        if (subscriptionId == null) {
            return;
        }
        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresent(this::applyRenewalFromWebhook);
    }

    private void onInvoicePaymentFailed(com.stripe.model.Event event) {
        String subscriptionId = extractSubscriptionId(event);
        if (subscriptionId == null) {
            return;
        }
        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresent(sub -> {
                    SubscriptionStatus prev = sub.getStatus();
                    sub.setStatus(SubscriptionStatus.PAST_DUE);
                    sub.setGraceUntil(LocalDateTime.now().plusDays(GRACE_DAYS));
                    audit(sub, SubscriptionActorType.SYSTEM, null, sub.getPlanType(), sub.getPlanType(),
                            prev, SubscriptionStatus.PAST_DUE, "Échec paiement renouvellement");
                    syncUserFromSubscription(sub.getUser(), sub);
                    subscriptionRepository.save(sub);
                });
    }

    private void onSubscriptionDeleted(com.stripe.model.Event event) {
        String subscriptionId = extractSubscriptionId(event);
        if (subscriptionId == null) {
            return;
        }
        subscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .ifPresent(sub -> forceDowngradeToFree(sub, "Abonnement Stripe supprimé après impayés"));
    }

    private void applyRenewalFromWebhook(SellerSubscription sub) {
        SellerPlan previous = sub.getPlanType();
        if (sub.getScheduledDowngrade() != null) {
            previous = sub.getPlanType();
            sub.setPlanType(sub.getScheduledDowngrade());
            sub.setScheduledDowngrade(null);
        }
        SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
        LocalDateTime now = LocalDateTime.now();
        sub.setStartDate(now);
        int months = sub.getBillingCycle() == PlanBillingCycle.ANNUAL ? 12 : 1;
        sub.setRenewalDate(now.plusMonths(months));
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setGraceUntil(null);
        sub.setDowngradeLocked(sub.getPlanType() != SellerPlan.FREE);
        sub.setBoostsRemaining(def.getMonthlyBoostsIncluded());
        sub.setCommissionRate(def.getCommissionPercent());
        audit(sub, SubscriptionActorType.SYSTEM, null, previous, sub.getPlanType(),
                SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE, "Renouvellement invoice.paid");
        syncUserFromSubscription(sub.getUser(), sub);
        subscriptionRepository.save(sub);
    }

    private String extractSubscriptionId(com.stripe.model.Event event) {
        try {
            com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (invoice != null && invoice.getSubscription() != null) {
                return invoice.getSubscription();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void onPaymentIntentSucceeded(com.stripe.model.Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (intent == null || intent.getMetadata() == null) {
            return;
        }
        String userIdStr = intent.getMetadata().get("userId");
        String targetPlanStr = intent.getMetadata().get("targetPlan");
        String billingStr = intent.getMetadata().get("billingCycle");
        String idem = intent.getMetadata().get("idempotencyKey");
        if (userIdStr == null || targetPlanStr == null || idem == null) {
            return;
        }
        User user = userRepository.findById(Long.parseLong(userIdStr)).orElse(null);
        if (user == null) {
            return;
        }
        SellerPlan plan = SellerPlan.valueOf(targetPlanStr);
        PlanBillingCycle cycle = billingStr != null ? PlanBillingCycle.valueOf(billingStr) : PlanBillingCycle.MONTHLY;
        applyPaidUpgrade(user, plan, cycle, intent.getAmount(), idem, intent.getId(), null,
                SubscriptionActorType.SYSTEM, null);
        pendingCheckoutRepository.findByCheckoutId(intent.getMetadata().get("checkoutId"))
                .ifPresent(pendingCheckoutRepository::delete);
    }

    public void forceDowngradeToFree(SellerSubscription sub, String reason) {
        SellerPlan previous = sub.getPlanType();
        sub.setPlanType(SellerPlan.FREE);
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setScheduledDowngrade(null);
        sub.setDowngradeLocked(false);
        sub.setBoostsRemaining(0);
        sub.setCommissionRate(SellerPlanCatalog.get(SellerPlan.FREE).getCommissionPercent());
        sub.setStartDate(null);
        sub.setRenewalDate(null);
        sub.setGraceUntil(null);
        pauseExcessPublications(sub.getUser().getId(), SellerPlanCatalog.get(SellerPlan.FREE).getMaxActivePublications());
        audit(sub, SubscriptionActorType.SYSTEM, null, previous, SellerPlan.FREE,
                SubscriptionStatus.PAST_DUE, SubscriptionStatus.CANCELLED, reason);
        syncUserFromSubscription(sub.getUser(), sub);
        subscriptionRepository.save(sub);
    }

    private void pauseExcessPublications(Long sellerId, int maxActive) {
        List<Annonce> active = annonceRepository.findBySellerId(sellerId, PageRequest.of(0, 500))
                .getContent().stream()
                .filter(a -> !a.isPlanPaused())
                .filter(a -> a.getStatus() == Annonce.Status.PENDING || a.getStatus() == Annonce.Status.APPROVED)
                .sorted(Comparator.comparing(Annonce::getCreatedAt))
                .toList();
        if (active.size() <= maxActive) {
            return;
        }
        int toPause = active.size() - maxActive;
        for (int i = active.size() - toPause; i < active.size(); i++) {
            Annonce a = active.get(i);
            a.setPlanPaused(true);
            annonceRepository.save(a);
        }
    }

    private SellerSubscription refreshState(SellerSubscription sub) {
        LocalDateTime now = LocalDateTime.now();
        if (sub.getPlanType() != SellerPlan.FREE
                && sub.getRenewalDate() != null
                && now.isAfter(sub.getRenewalDate())
                && (sub.getGraceUntil() == null || now.isAfter(sub.getGraceUntil()))
                && sub.getStatus() != SubscriptionStatus.PAST_DUE) {
            forceDowngradeToFree(sub, "Période expirée sans renouvellement");
        } else {
            sub.setDowngradeLocked(sub.getPlanType() != SellerPlan.FREE
                    && sub.getRenewalDate() != null
                    && now.isBefore(sub.getRenewalDate()));
        }
        return subscriptionRepository.save(sub);
    }

    private SellerSubscription createFromUser(User user) {
        SellerSubscription sub = new SellerSubscription();
        sub.setUser(user);
        sub.setPlanType(user.getSellerPlan() != null ? user.getSellerPlan() : SellerPlan.FREE);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(user.getPlanPeriodStart());
        sub.setRenewalDate(user.getPlanPeriodEnd());
        sub.setStripeSubscriptionId(user.getStripeSubscriptionId());
        sub.setBoostsRemaining(user.getBoostsRemaining());
        SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
        sub.setCommissionRate(def.getCommissionPercent());
        sub.setBillingCycle(user.getPlanBillingCycle());
        sub.setGraceUntil(user.getPlanGraceUntil());
        sub.setDowngradeLocked(sub.getPlanType() != SellerPlan.FREE
                && sub.getRenewalDate() != null
                && LocalDateTime.now().isBefore(sub.getRenewalDate()));
        return subscriptionRepository.save(sub);
    }

    private SellerSubscription lockSubscription(Long userId, Long expectedVersion) {
        SellerSubscription sub = subscriptionRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Abonnement introuvable"));
        if (expectedVersion != null && !expectedVersion.equals(sub.getVersion())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Votre abonnement a été modifié ailleurs. Rafraîchissez la page et réessayez.");
        }
        return sub;
    }

    private void recordFinancial(
            SellerSubscription sub,
            SubscriptionFinancialType type,
            long amountCents,
            String idempotencyKey,
            String paymentIntentId,
            String invoiceId) {
        SubscriptionFinancialTransaction tx = new SubscriptionFinancialTransaction();
        tx.setSubscription(sub);
        tx.setTransactionType(type);
        tx.setAmountCents(amountCents);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStripePaymentIntentId(paymentIntentId);
        tx.setStripeInvoiceId(invoiceId);
        financialRepository.save(tx);
    }

    private void audit(
            SellerSubscription sub,
            SubscriptionActorType actor,
            Long actorUserId,
            SellerPlan prevPlan,
            SellerPlan newPlan,
            SubscriptionStatus prevStatus,
            SubscriptionStatus newStatus,
            String detail) {
        SubscriptionAuditLog log = new SubscriptionAuditLog();
        log.setSubscription(sub);
        log.setActorType(actor);
        log.setActorUserId(actorUserId);
        log.setPreviousPlan(prevPlan);
        log.setNewPlan(newPlan);
        log.setPreviousStatus(prevStatus);
        log.setNewStatus(newStatus);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    private SellerSubscriptionStatusDTO toStatusDto(User user, SellerSubscription sub) {
        SellerPlanDefinition def = SellerPlanCatalog.get(sub.getPlanType());
        long active = annonceRepository.countActivePublicationsBySeller(user.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean grace = sub.getGraceUntil() != null && !now.isAfter(sub.getGraceUntil());
        boolean periodActive = isSubscriptionPeriodActive(sub);
        String scheduledLabel = null;
        if (sub.getScheduledDowngrade() != null) {
            scheduledLabel = SellerPlanCatalog.get(sub.getScheduledDowngrade()).getLabel();
        }
        return new SellerSubscriptionStatusDTO(
                sub.getPlanType().name(),
                def.getLabel(),
                def.getCommissionPercent(),
                def.getMaxActivePublications(),
                def.isUnlimitedPublications(),
                active,
                sub.getBoostsRemaining(),
                def.getMonthlyBoostsIncluded(),
                sub.getBillingCycle() != null ? sub.getBillingCycle().name() : null,
                sub.getStartDate(),
                sub.getRenewalDate(),
                sub.getGraceUntil(),
                grace,
                user.getCreditBalance() != null ? user.getCreditBalance() : BigDecimal.ZERO,
                periodActive,
                periodActive && (def.isUnlimitedPublications() || active < def.getMaxActivePublications()),
                sub.getStatus().name(),
                sub.getScheduledDowngrade() != null ? sub.getScheduledDowngrade().name() : null,
                scheduledLabel,
                sub.isDowngradeLocked(),
                sub.getVersion());
    }

    private void assertVendor(User seller) {
        if (seller.getRole() != User.Role.VENDEUR && seller.getRole() != User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux vendeurs");
        }
    }

    private boolean isStripeConfigured() {
        return stripeSecretKey != null
                && !stripeSecretKey.isBlank()
                && !stripeSecretKey.contains("your_stripe");
    }

    public static boolean isOptimisticLockConflict(Throwable t) {
        return t instanceof OptimisticLockingFailureException
                || t instanceof ObjectOptimisticLockingFailureException;
    }
}
