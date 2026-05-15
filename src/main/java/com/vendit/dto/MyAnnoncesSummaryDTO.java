package com.vendit.dto;

import lombok.Data;

/** Agrégats « mes annonces » pour le vendeur (hors pagination liste). */
@Data
public class MyAnnoncesSummaryDTO {
    private long totalCount;
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private long soldCount;
    private long expiredCount;
    private long totalViews;
    private long totalContacts;
}
