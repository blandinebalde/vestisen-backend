package com.vestisen.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Provider JWT pour la génération et la validation des tokens
 */
@Component
public class JwtTokenProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    /**
     * Génère la clé de signature à partir du secret
     * Pour HS512, la clé doit faire au moins 512 bits (64 caractères)
     * Pour HS256, la clé doit faire au moins 256 bits (32 caractères)
     */
    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
        }
        
        // Vérifier que la clé est assez longue pour HS512 (64 caractères minimum)
        if (jwtSecret.length() < 64) {
            logger.warn("JWT secret is less than 64 characters. Consider using a longer secret for HS512 algorithm.");
        }
        
        // Générer une clé de 512 bits minimum pour HS512
        // Si le secret est plus court, on le répète ou on utilise HS256
        byte[] keyBytes = jwtSecret.getBytes();
        if (keyBytes.length < 64) {
            // Pour HS512, on a besoin d'au moins 64 bytes
            // On peut soit utiliser HS256, soit étendre la clé
            // Ici, on va utiliser HS256 qui est plus standard et suffisant
            logger.info("Using HS256 algorithm due to key length");
            return Keys.hmacShaKeyFor(keyBytes);
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Génère un token JWT pour l'utilisateur authentifié
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        // Utiliser HS256 qui est plus standard et nécessite seulement 256 bits (32 caractères)
        // HS512 nécessite 512 bits (64 caractères) minimum
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
    
    /**
     * Extrait le nom d'utilisateur (email) depuis le token
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (JwtException e) {
            logger.error("Error extracting username from token", e);
            throw e;
        }
    }
    
    /**
     * Valide le token JWT
     * @return true si le token est valide, false sinon
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }
    
    /**
     * Extrait la date d'expiration du token
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
        } catch (JwtException e) {
            logger.error("Error extracting expiration date from token", e);
            return null;
        }
    }
    
    /**
     * Vérifie si le token est expiré
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }
}
