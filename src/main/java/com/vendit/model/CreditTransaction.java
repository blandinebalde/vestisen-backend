package com.vendit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historique d'achat de crédits : l'utilisateur paie en FCFA (carte, Wave, etc.) et reçoit des crédits.
 */
@Entity
@Table(name = "credit_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant public (API, URLs) — non séquentiel. */
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", length = 36, unique = true, nullable = true, updatable = false)
    private UUID publicId;

    /** Code unique de la transaction (18 caractères alphanumériques). */
    @Column(unique = true, length = 18)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Montant payé en FCFA */
    @Column(name = "amount_fcfa", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountFcfa;

    /** Nombre de crédits ajoutés au solde */
    @Column(name = "credits_added", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditsAdded;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private String transactionId;
    private String paymentProviderId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime paidAt;

    public enum PaymentMethod {
        STRIPE, WAVE, ORANGE_MONEY, CARD
    }

    public enum Status {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
