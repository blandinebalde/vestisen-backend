package com.vendit.event;

import org.springframework.context.ApplicationEvent;

import com.vendit.model.Annonce;

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
