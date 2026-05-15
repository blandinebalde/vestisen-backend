package com.vendit.config;

/**
 * Bornes catalogue pour éviter requêtes sans limite (cible volumétrie 10M+ lignes).
 */
public final class CatalogPageLimits {

    /** Taille max d'une page liste catalogue / admin annonces. */
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;
    /** Traitement des annonces expirées par lots (job planifié). */
    public static final int EXPIRED_ANNONCE_BATCH_SIZE = 500;
    /** « Top » homepage / API public/top. */
    public static final int MAX_TOP_LIMIT = 100;
    /** Liste « mes achats » (borne serveur). */
    public static final int MY_PURCHASES_MAX = 100;

    private CatalogPageLimits() {
    }

    public static int clampPageSize(int requested) {
        if (requested <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requested, MAX_PAGE_SIZE);
    }

    public static int clampPageIndex(int page) {
        return Math.max(0, page);
    }

    public static int clampTopLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, MAX_TOP_LIMIT);
    }
}
