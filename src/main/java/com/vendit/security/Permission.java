package com.vendit.security;

/**
 * Autorités fines ({@code perm:…}) injectées dans le {@link org.springframework.security.core.userdetails.UserDetails}
 * en plus du rôle {@code ROLE_*}. Utiliser avec {@code @PreAuthorize("hasAuthority('perm:…')")}.
 */
public enum Permission {

    /** Accès à l’API d’administration ({@code /api/admin/**}). */
    ADMIN_FULL("perm:admin:full"),

    /** Création d’annonce (vendeur / admin). */
    ANNONCE_CREATE("perm:annonce:create"),

    /** Gestion du catalogue vendeur : mes annonces, photos. */
    ANNONCE_SELLER_READ("perm:annonce:seller_read"),

    /** Modifier / supprimer ses annonces (hors cas réservés, ex. vendue). */
    ANNONCE_SELLER_WRITE("perm:annonce:seller_write"),

    /** Contacter le vendeur depuis une fiche. */
    MARKET_CONTACT("perm:market:contact"),

    /** Achat côté client : panier finalisé, mes achats. */
    MARKET_BUY("perm:market:buy"),

    /** Consulter son solde de crédits. */
    CREDIT_BALANCE_READ("perm:credit:balance_read"),

    /** Achat de crédits, historique et confirmation de transaction. */
    CREDIT_VENDOR("perm:credit:vendor"),

    CART_USE("perm:cart:use"),
    CONVERSATION_USE("perm:conversation:use"),
    REVIEW_READ("perm:review:read"),
    REVIEW_WRITE("perm:review:write"),
    PAYMENT_USE("perm:payment:use"),
    PROFILE_ACCESS("perm:profile:access");

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}
