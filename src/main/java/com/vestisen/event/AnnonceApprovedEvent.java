package com.vestisen.event;

import com.vestisen.model.Annonce;
import org.springframework.context.ApplicationEvent;

/**
 * Événement publié lorsqu’une annonce est acceptée (statut APPROVED).
 * L’observer utilise cet événement pour définir publishedAt et expiresAt
 * (décompte à partir de l’acceptation).
 */
public class AnnonceApprovedEvent extends ApplicationEvent {

    private final Annonce annonce;

    public AnnonceApprovedEvent(Object source, Annonce annonce) {
        super(source);
        this.annonce = annonce;
    }

    public Annonce getAnnonce() {
        return annonce;
    }
}
