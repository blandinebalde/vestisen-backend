package com.vendit.security;

import java.util.Date;

/**
 * Données extraites d'un JWT d'accès après vérification de signature et dates.
 */
public record ParsedAccessToken(String subject, String jti, long tokenVersion, Date expiration) {
}
