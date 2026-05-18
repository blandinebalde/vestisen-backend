package com.vendit.observer;

import java.math.BigDecimal;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vendit.event.AnnonceApprovedEvent;
import com.vendit.model.Annonce;
import com.vendit.model.CreditUsage;
import com.vendit.model.PublicationTarif;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.CreditUsageRepository;
import com.vendit.repository.PublicationTarifRepository;
import com.vendit.service.ActionLogService;

import java.time.LocalDateTime;

/**
 * Observer qui met à jour publishedAt et expiresAt dès qu’une annonce est acceptée.
 * Le décompte de la durée de publication commence à l’acceptation (publishedAt).
 */
@Component
public class AnnonceApprovalObserver {

    private final AnnonceRepository annonceRepository;
    private final PublicationTarifRepository tarifRepository;
    private final CreditUsageRepository creditUsageRepository;
    private final ActionLogService actionLogService;

    public AnnonceApprovalObserver(AnnonceRepository annonceRepository,
                                  PublicationTarifRepository tarifRepository,
                                  CreditUsageRepository creditUsageRepository,
                                  ActionLogService actionLogService) {
        this.annonceRepository = annonceRepository;
        this.tarifRepository = tarifRepository;
        this.creditUsageRepository = creditUsageRepository;
        this.actionLogService = actionLogService;
    }

    @EventListener
    @Transactional
    public void onAnnonceApproved(AnnonceApprovedEvent event) {
        Annonce annonce = event.getAnnonce();
        if (annonce.getStatus() != Annonce.Status.APPROVED) return;
        if (annonce.getPublishedAt() != null) return; // déjà traité

        LocalDateTime now = LocalDateTime.now();
        annonce.setPublishedAt(now);

        PublicationTarif tarif = tarifRepository.findByTypeNameAndActiveTrue(annonce.getPublicationType()).orElse(null);
        int days = com.vendit.service.SellerPlanService.MAX_ANNONCE_LIFETIME_DAYS;
        if (tarif != null && tarif.getDurationDays() != null && tarif.getDurationDays() > 0) {
            days = Math.min(tarif.getDurationDays(), com.vendit.service.SellerPlanService.MAX_ANNONCE_LIFETIME_DAYS);
        }
        annonce.setExpiresAt(now.plusDays(days));

        annonceRepository.save(annonce);
        User seller = annonce.getSeller();
        BigDecimal creditsUsed = annonce.getPublicationCreditCost() != null ? annonce.getPublicationCreditCost() : BigDecimal.ZERO;
        if (seller != null && creditsUsed.compareTo(BigDecimal.ZERO) > 0) {
            CreditUsage usage = new CreditUsage();
            usage.setUserId(seller.getId());
            usage.setAnnonceId(annonce.getId());
            usage.setCreditsUsed(creditsUsed);
            usage.setUsageType("PUBLICATION_ACCEPTED");
            creditUsageRepository.save(usage);
        }
        actionLogService.logInternalAction(
                seller != null ? seller.getId() : null,
                seller != null ? seller.getEmail() : "system",
                seller != null && seller.getRole() != null ? seller.getRole().name() : null,
                "Publication acceptée - décompte démarré",
                "annonce",
                annonce.getId(),
                true);
    }
}
