package com.vendit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Jeton d'accès révoqué (logout) : conservé jusqu'à l'expiration naturelle du JWT pour refuser les rejeux.
 */
@Entity
@Table(name = "revoked_jwt_tokens", indexes = {
        @Index(name = "idx_revoked_jti", columnList = "jti", unique = true),
        @Index(name = "idx_revoked_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
public class RevokedJwtToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RevokedJwtToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }
}
