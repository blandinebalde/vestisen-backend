package com.vendit.security;

import com.vendit.model.RevokedJwtToken;
import com.vendit.repository.RevokedJwtTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT : durée configurable, identifiant de jeton (jti), version pour invalidation globale,
 * révocation par liste noire (logout).
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String CLAIM_TOKEN_VERSION = "tv";

    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Durée de vie en millisecondes ({@code jwt.expiration-ms} ou repli sur {@code jwt.expiration}). */
    @Value("${jwt.expiration-ms:${jwt.expiration}}")
    private long jwtExpirationMs;

    @Autowired
    private RevokedJwtTokenRepository revokedJwtTokenRepository;

    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
        }
        if (jwtSecret.length() < 64) {
            logger.warn("JWT secret is less than 64 characters. Prefer a longer secret for HS256.");
        }
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessTokenValidityMs() {
        return jwtExpirationMs > 0 ? jwtExpirationMs : 86_400_000L;
    }

    public long getAccessTokenValiditySeconds() {
        return Math.max(1L, getAccessTokenValidityMs() / 1000L);
    }

    /**
     * Génère un JWT avec {@code jti} unique, claim {@code tv} (version) pour invalidation globale.
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + getAccessTokenValidityMs());
        String jti = UUID.randomUUID().toString().replace("-", "");
        long tv = 0L;
        if (userDetails instanceof AppUserDetails aud) {
            tv = aud.getTokenVersion();
        }
        return Jwts.builder()
                .id(jti)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim(CLAIM_TOKEN_VERSION, tv)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Signature valide, non expiré, non révoqué (liste noire).
     */
    public boolean validateToken(String token) {
        return parseSignedAccessToken(token)
                .filter(p -> !revokedJwtTokenRepository.existsByJti(p.jti()))
                .isPresent();
    }

    /**
     * Parse et vérifie signature + dates ; ne vérifie pas la liste noire (usage interne avant insertion).
     */
    public Optional<ParsedAccessToken> parseSignedAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String jti = claims.getId();
            if (jti == null || jti.isBlank()) {
                return Optional.empty();
            }
            Object rawTv = claims.get(CLAIM_TOKEN_VERSION);
            long tv = 0L;
            if (rawTv instanceof Number n) {
                tv = n.longValue();
            }
            Date exp = claims.getExpiration();
            if (exp != null && exp.before(new Date())) {
                return Optional.empty();
            }
            return Optional.of(new ParsedAccessToken(claims.getSubject(), jti, tv, exp));
        } catch (ExpiredJwtException e) {
            logger.debug("Expired JWT");
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature");
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT");
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT validation error: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Jeton utilisable pour authentifier la requête : signature, dates, pas révoqué.
     */
    public Optional<ParsedAccessToken> parseAndValidateAccessToken(String token) {
        return parseSignedAccessToken(token)
                .filter(p -> !revokedJwtTokenRepository.existsByJti(p.jti()));
    }

    public String getUsernameFromToken(String token) {
        return parseSignedAccessToken(token)
                .map(ParsedAccessToken::subject)
                .orElseThrow(() -> new IllegalArgumentException("Cannot read subject from token"));
    }

    public Date getExpirationDateFromToken(String token) {
        return parseSignedAccessToken(token)
                .map(ParsedAccessToken::expiration)
                .orElse(null);
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }

    /**
     * Révoque ce jeton (logout) : refusé jusqu'à son expiration naturelle.
     */
    @Transactional
    public void revokeAccessToken(String token) {
        parseSignedAccessToken(token).ifPresent(p -> {
            if (revokedJwtTokenRepository.existsByJti(p.jti())) {
                return;
            }
            Instant until = p.expiration() != null ? p.expiration().toInstant() : Instant.now().plusMillis(getAccessTokenValidityMs());
            revokedJwtTokenRepository.save(new RevokedJwtToken(p.jti(), until));
        });
    }
}
