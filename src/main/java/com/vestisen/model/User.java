package com.vestisen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", schema = "vendit", catalog = "vendit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
    
    /** Solde de crédits (pour publier des annonces). Les types de publication sont payés en crédits. */
    @Column(name = "credit_balance", nullable = false, precision = 12, scale = 2, columnDefinition = "DECIMAL(12,2) NOT NULL DEFAULT 0")
    private BigDecimal creditBalance = BigDecimal.ZERO;
    
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
