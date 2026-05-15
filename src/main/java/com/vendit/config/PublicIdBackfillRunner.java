package com.vendit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Renseigne {@code public_id} (UUID) pour les lignes existantes après ajout de colonne (ddl-auto).
 * Idempotent : ne met à jour que les lignes où {@code public_id} est NULL.
 */
@Component
@Order(50)
public class PublicIdBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PublicIdBackfillRunner.class);

    @Autowired
    private PublicIdBackfillService backfillService;

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        String uuidExpr = uuidGeneratorSql();
        String[] tables = {
                "credit_ledger_entries",
                "credit_transactions",
                "payments",
                "conversations",
                "annonces",
                "users"
        };
        for (String table : tables) {
            try {
                int n = backfillService.backfillPublicIds(table, uuidExpr);
                if (n > 0) {
                    log.info("public_id backfill: {} row(s) updated in {}", n, table);
                }
            } catch (Exception e) {
                log.warn("public_id backfill skipped for {}: {}", table, e.getMessage());
            }
        }
    }

    private String uuidGeneratorSql() {
        try (Connection c = dataSource.getConnection()) {
            String product = c.getMetaData().getDatabaseProductName();
            if (product != null && product.toLowerCase().contains("postgres")) {
                return "gen_random_uuid()";
            }
        } catch (SQLException e) {
            log.warn("Could not detect DB product for public_id backfill, defaulting to MySQL UUID(): {}", e.getMessage());
        }
        return "UUID()";
    }
}
