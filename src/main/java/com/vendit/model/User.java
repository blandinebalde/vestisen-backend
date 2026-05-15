package com.vendit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners({AuditingEntityListener.class, UserPrivilegeEntityListener.class})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", length = 36, unique = true, nullable = true, updatable = false)
    private UUID publicId;

    /** Code unique de l'utilisateur (18 caractères alphanumériques). */
    @Column(unique = true, length = 18)
    private String code;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(unique = true)
    private String phone;
    
    private String address;
    
    private String whatsapp;

    /** Chemin relatif de la photo de profil (ex: profile/user/CODE/xxx.jpg). */
    @Column(name = "avatar_path", length = 512)
    private String avatarPath;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private Role role = Role.USER;
    
    private boolean enabled = true;
    
    @Column(nullable = false)
    private boolean emailVerified = false;
    
    @Column(name = "verification_token", length = 255)
    private String verificationToken;
    
    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;
    
    @Column(name = "reset_password_token", length = 255)
    private String resetPasswordToken;
    
    @Column(name = "reset_password_expiry")
    private LocalDateTime resetPasswordExpiry;
    
    /**
     * Incrémenté pour invalider tous les JWT encore valides cryptographiquement
     * (ex. après changement de mot de passe).
     */
    @Column(name = "token_version", nullable = false)
    private long tokenVersion = 0L;

    /**
     * HMAC des attributs de privilège (rôle, activation, etc.). Recalculé par {@link UserPrivilegeEntityListener}
     * avant chaque persistance ; vérifié à l'authentification.
     */
    @Column(name = "privilege_seal", length = 64)
    private String privilegeSeal;

    /** Solde de crédits (pour publier des annonces). Les types de publication sont payés en crédits. */
    @Column(name = "credit_balance", nullable = false, precision = 12, scale = 2, columnDefinition = "DECIMAL(12,2) NOT NULL DEFAULT 0")
    private BigDecimal creditBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_plan", nullable = false, length = 20)
    private SellerPlan sellerPlan = SellerPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_billing_cycle", length = 20)
    private PlanBillingCycle planBillingCycle;

    @Column(name = "plan_period_start")
    private LocalDateTime planPeriodStart;

    @Column(name = "plan_period_end")
    private LocalDateTime planPeriodEnd;

    /** Fin de période de grâce après échec de paiement abonnement (rétrogradation ensuite). */
    @Column(name = "plan_grace_until")
    private LocalDateTime planGraceUntil;

    @Column(name = "stripe_subscription_id", length = 128)
    private String stripeSubscriptionId;

    @Column(name = "boosts_remaining", nullable = false)
    private int boostsRemaining = 0;

    @Column(name = "boosts_period_start")
    private LocalDateTime boostsPeriodStart;
    
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Annonce> annonces = new HashSet<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum Role {
        ADMIN, VENDEUR, USER
    }
}
