package com.vendit.model;

/**
 * Type de mouvement sur le grand livre des crédits (solde utilisateur).
 */
public enum CreditLedgerMovementType {
    /** Crédits ajoutés suite à un achat confirmé. */
    CREDIT_PURCHASE,
    /** Crédits débités lors de la création d'une annonce (publication). */
    DEBIT_PUBLICATION
}
