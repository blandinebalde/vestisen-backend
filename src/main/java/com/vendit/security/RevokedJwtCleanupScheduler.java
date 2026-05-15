package com.vendit.security;

import com.vendit.repository.RevokedJwtTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Supprime les entrées de révocation expirées (plus nécessaires une fois le JWT passé).
 */
@Component
public class RevokedJwtCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RevokedJwtCleanupScheduler.class);

    @Autowired
    private RevokedJwtTokenRepository revokedJwtTokenRepository;

    @Scheduled(cron = "${jwt.revoked-cleanup-cron:0 15 3 * * *}")
    @Transactional
    public void purgeExpiredRevocations() {
        int n = revokedJwtTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (n > 0) {
            log.debug("Purged {} expired revoked JWT entries", n);
        }
    }
}
