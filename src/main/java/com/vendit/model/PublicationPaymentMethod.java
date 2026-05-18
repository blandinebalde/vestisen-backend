package com.vendit.model;

/** Comment la publication est payée à la création d'annonce. */
public enum PublicationPaymentMethod {
    /** Débit du solde crédits selon le tarif de visibilité. */
    CREDITS,
    /** Utilise un emplacement du quota abonnement (sans débit crédits). */
    SUBSCRIPTION
}
