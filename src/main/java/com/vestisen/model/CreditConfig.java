package com.vestisen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuration des crédits : prix en FCFA pour 1 crédit.
 * Les utilisateurs achètent des crédits (carte, Wave, etc.) et dépensent des crédits pour publier selon le type d'annonce.
 */
@Entity
@Table(name = "credit_config", schema = "vendit", catalog = "vendit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CreditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Prix en FCFA pour 1 crédit (ex: 100 = 1 crédit coûte 100 FCFA) */
    @Column(name = "price_per_credit_fcfa", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerCreditFcfa = new BigDecimal("100");

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
