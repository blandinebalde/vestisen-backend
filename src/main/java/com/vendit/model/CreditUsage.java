package com.vendit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historique d'utilisation des crédits par les utilisateurs (ex: à chaque publication acceptée).
 * Permet d'analyser et d'auditer l'utilisation des crédits par user.
 */
@Entity
@Table(name = "credit_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CreditUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "annonce_id")
    private Long annonceId;

    /** Nombre de crédits utilisés pour cette action */
    @Column(name = "credits_used", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditsUsed;

    /** Type d'usage (ex: PUBLICATION_ACCEPTED) */
    @Column(name = "usage_type", nullable = false, length = 64)
    private String usageType = "PUBLICATION_ACCEPTED";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
