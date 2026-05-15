package com.vendit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.vendit.model.Category;
import com.vendit.model.CreditConfig;
import com.vendit.model.PublicationTarif;
import com.vendit.repository.CategoryRepository;
import com.vendit.repository.CreditConfigRepository;
import com.vendit.repository.PublicationTarifRepository;
import com.vendit.repository.UserRepository;
import com.vendit.service.UserService;

import java.math.BigDecimal;

/**
 * Au démarrage (après Hibernate) : si la base est vide ou quasi vide, insère admin (via UserService),
 * catégories par défaut, config crédits, tarifs, et comptes de démo vendeur / client.
 */
@Component
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PublicationTarifRepository tarifRepository;

    @Autowired
    private CreditConfigRepository creditConfigRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Value("${app.security.auto-repair-privilege-seals:true}")
    private boolean autoRepairPrivilegeSeals;

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                long userCount = userRepository.count();
                long categoryCount = categoryRepository.count();
                boolean emptyOrFresh = userCount == 0 || categoryCount == 0;
                if (emptyOrFresh) {
                    logger.info("Seeding defaults (users={}, categories={})...", userCount, categoryCount);
                }

                userService.initializeAdmin();
                userService.ensureSeedDemoUsers();
                createDefaultCategories();
                createDefaultCreditConfig();
                createDefaultTarifs();

                if (autoRepairPrivilegeSeals) {
                    userService.repairUsersWithMissingPrivilegeSeals();
                }

                if (emptyOrFresh) {
                    logger.info("Seed data applied for empty or partial database.");
                }
                logger.info("DataInitializer completed successfully");
                return;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean tableMissing = msg.contains("exist") || msg.contains("doesn't exist") || msg.contains("Unknown table");
                if (tableMissing && attempt < MAX_RETRIES) {
                    logger.warn("DataInitializer attempt {}/{} failed (tables may not be ready yet): {}. Retrying in {} ms...",
                            attempt, MAX_RETRIES, e.getMessage(), RETRY_DELAY_MS);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("DataInitializer interrupted");
                        return;
                    }
                } else {
                    logger.error("DataInitializer failed after {} attempt(s): {}", attempt, e.getMessage(), e);
                    if (tableMissing) {
                        logger.error("La table 'users' (ou une autre) est absente. Vérifiez que la base de données 'vendit' existe et que Hibernate a créé les tables. " +
                                "Si vous avez supprimé les tables, lancez une fois avec: spring.jpa.hibernate.ddl-auto=create (puis remettez 'update')");
                    }
                    return;
                }
            }
        }
    }
    
    private void createDefaultCreditConfig() {
        if (creditConfigRepository.count() == 0) {
            CreditConfig config = new CreditConfig();
            config.setPricePerCreditFcfa(new BigDecimal("100"));
            creditConfigRepository.save(config);
        }
    }
    
    private void createDefaultCategories() {
        String[][] defaults = {
            {"Vêtements femme", "Vêtements pour femmes", "👗"},
            {"Vêtements homme", "Vêtements pour hommes", "👔"},
            {"Accessoires", "Sacs, bijoux, chaussures...", "👜"},
            {"Promotion", "Offres et lots", "🏷️"},
            {"Électronique", "Téléphones, ordinateurs...", "📱"},
            {"Maison", "Décoration, mobilier...", "🏠"}
        };
        for (String[] row : defaults) {
            if (!categoryRepository.existsByName(row[0])) {
                Category c = new Category();
                c.setName(row[0]);
                c.setDescription(row[1]);
                c.setIcon(row[2]);
                c.setActive(true);
                categoryRepository.save(c);
            }
        }
    }
    
    private void createDefaultTarifs() {
        // Coût en crédits (les utilisateurs achètent des crédits puis dépensent pour publier)
        createTarif("Standard", new BigDecimal("5"), 7);
        createTarif("Premium", new BigDecimal("15"), 14);
        createTarif("Top Pub", new BigDecimal("30"), 30);
    }
    
   
    private void createTarif(String typeName, BigDecimal price, int durationDays) {
        if (tarifRepository.findByTypeName(typeName).isEmpty()) {
            PublicationTarif tarif = new PublicationTarif();
            tarif.setTypeName(typeName);
            tarif.setPrice(price);
            tarif.setDurationDays(durationDays);
            tarif.setActive(true);
            tarifRepository.save(tarif);
        }
    }
}
