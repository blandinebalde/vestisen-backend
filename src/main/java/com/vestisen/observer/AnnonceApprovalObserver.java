package com.vestisen.observer;

import com.vestisen.event.AnnonceApprovedEvent;
import com.vestisen.model.Annonce;
import com.vestisen.model.PublicationTarif;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.PublicationTarifRepository;
import com.vestisen.service.ActionLogService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Observer qui met à jour publishedAt et expiresAt dès qu’une annonce est acceptée.
 * Le décompte de la durée de publication commence à l’acceptation (publishedAt).
 */
@Component
public class AnnonceApprovalObserver {

    private final AnnonceRepository annonceRepository;
    private final PublicationTarifRepository tarifRepository;
    private final ActionLogService actionLogService;

    public AnnonceApprovalObserver(AnnonceRepository annonceRepository,
                                  PublicationTarifRepository tarifRepository,
                                  ActionLogService actionLogService) {
        this.annonceRepository = annonceRepository;
        this.tarifRepository = tarifRepository;
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
        Integer days = tarif != null ? tarif.getDurationDays() : null;
        if (days != null && days > 0) {
            annonce.setExpiresAt(now.plusDays(days));
        } else {
            annonce.setExpiresAt(null);
        }

        annonceRepository.save(annonce);
        User seller = annonce.getSeller();
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
