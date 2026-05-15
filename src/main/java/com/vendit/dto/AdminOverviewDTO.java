package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicateurs opérationnels pour le tableau de bord administrateur (tous calculés côté serveur).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewDTO {
    /** Modération */
    private long annoncesPending;
    private long annoncesPendingOlderThan7Days;
    private long annoncesApproved;
    private long annoncesRejected;
    private long annoncesSold;
    private long annoncesExpired;
    private long totalAnnonces;
    private long annoncesCreatedThisMonth;

    /** Comptes */
    private long usersTotal;
    private long usersVendeurs;
    private long usersClients;
    private long usersAdmins;
    private long usersDisabled;
    private long usersEmailUnverified;

    /** Économie crédits (plateforme) */
    private BigDecimal creditsPurchased;
    private BigDecimal creditsSpent;
    private BigDecimal revenueFcfaTotal;
    private BigDecimal revenueFcfaThisMonth;
    private long pendingCreditTransactions;

    /** Engagement catalogue (annonces approuvées) */
    private long engagementTotalApproved;
    private long totalViews;
    private long totalContacts;
    private double contactRatePercent;

    private List<StatsAnnoncesByCategoryDTO> topCategories;
    private List<AdminPendingAnnonceDTO> oldestPendingAnnonces;
}
