package com.vestisen.config;

import com.vestisen.model.Annonce;
import com.vestisen.model.PublicationTarif;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.PublicationTarifRepository;
import com.vestisen.repository.UserRepository;
import com.vestisen.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

    @Component
    public class DataInitializer implements CommandLineRunner {
    
    private boolean initialized = false;
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PublicationTarifRepository tarifRepository;
    
    @Autowired
    private AnnonceRepository annonceRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Initialiser l'admin
            userService.initializeAdmin();
            
           
        } catch (Exception e) {
            logger.error("Error in DataInitializer.run(): {}", e.getMessage(), e);
        }
    }
    
   
    private void createTarif(Annonce.PublicationType type, BigDecimal price, int durationDays) {
        if (tarifRepository.findByPublicationType(type).isEmpty()) {
            PublicationTarif tarif = new PublicationTarif();
            tarif.setPublicationType(type);
            tarif.setPrice(price);
            tarif.setDurationDays(durationDays);
            tarif.setActive(true);
            tarifRepository.save(tarif);
        }
    }
    
    private void createSampleUsers() {
        // Créer un vendeur de démonstration
        if (!userRepository.existsByEmail("vendeur@vestisen.com")) {
            User vendeur = new User();
            vendeur.setEmail("vendeur@vestisen.com");
            vendeur.setPassword(passwordEncoder.encode("vendeur123"));
            vendeur.setFirstName("Aminata");
            vendeur.setLastName("Diallo");
            vendeur.setPhone("+221771234567");
            vendeur.setAddress("Dakar, Sénégal");
            vendeur.setRole(User.Role.VENDEUR);
            vendeur.setEnabled(true);
            userRepository.save(vendeur);
        }
        
        // Créer un utilisateur de démonstration
        if (!userRepository.existsByEmail("user@vestisen.com")) {
            User user = new User();
            user.setEmail("user@vestisen.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setFirstName("Ibrahima");
            user.setLastName("Ndiaye");
            user.setPhone("+221775678901");
            user.setAddress("Thiès, Sénégal");
            user.setRole(User.Role.USER);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }
    
    private void createSampleAnnonces() {
        // Check if table exists and is empty
        try {
            long count = annonceRepository.count();
            if (count > 0) {
                logger.info("Annonces table already has {} records, skipping sample data", count);
                return; // Already has data
            }
        } catch (Exception e) {
            logger.error("Error checking annonces count - table may not exist yet: {}", e.getMessage());
            // Réessayer après un court délai
            try {
                Thread.sleep(500);
                long count = annonceRepository.count();
                if (count > 0) {
                    logger.info("Annonces table has {} records after retry", count);
                    return;
                }
            } catch (Exception retryException) {
                logger.error("Table 'annonces' does not exist. Please ensure Hibernate has created all tables. Error: {}", retryException.getMessage());
                throw new RuntimeException("Cannot create sample annonces: table does not exist", retryException);
            }
        }
        
        User vendeur = userRepository.findByEmail("vendeur@vestisen.com")
                .orElse(userRepository.findByEmail("admin@vestisen.com").orElse(null));
        
        if (vendeur != null) {
                // Annonce 1: Robe traditionnelle
                Annonce annonce1 = new Annonce();
                annonce1.setTitle("Belle robe traditionnelle sénégalaise");
                annonce1.setDescription("Magnifique robe traditionnelle en bazin riche, parfaite pour les cérémonies. Taille M, couleur bleu marine avec broderies dorées.");
                annonce1.setPrice(new BigDecimal("25000"));
                annonce1.setCategory(Annonce.Category.FEMME);
                annonce1.setPublicationType(Annonce.PublicationType.STANDARD);
                annonce1.setCondition(Annonce.Condition.NEUF);
                annonce1.setSize("M");
                annonce1.setBrand("Artisanat Local");
                annonce1.setColor("Bleu marine");
                annonce1.setLocation("Dakar");
                annonce1.setImages(Arrays.asList("https://via.placeholder.com/400x400?text=Robe+Traditionnelle"));
                annonce1.setSeller(vendeur);
                annonce1.setStatus(Annonce.Status.APPROVED);
                annonce1.setViewCount(45);
                annonce1.setContactCount(8);
                annonce1.setPublishedAt(LocalDateTime.now().minusDays(5));
                annonce1.setExpiresAt(LocalDateTime.now().plusDays(25));
                annonceRepository.save(annonce1);
                
                // Annonce 2: Chemise homme
                Annonce annonce2 = new Annonce();
                annonce2.setTitle("Chemise élégante pour homme");
                annonce2.setDescription("Chemise de qualité supérieure, 100% coton, parfaite pour le bureau ou les occasions spéciales.");
                annonce2.setPrice(new BigDecimal("15000"));
                annonce2.setCategory(Annonce.Category.HOMME);
                annonce2.setPublicationType(Annonce.PublicationType.PREMIUM);
                annonce2.setCondition(Annonce.Condition.TRES_BON_ETAT);
                annonce2.setSize("L");
                annonce2.setBrand("Zara");
                annonce2.setColor("Blanc");
                annonce2.setLocation("Dakar");
                annonce2.setImages(Arrays.asList("https://via.placeholder.com/400x400?text=Chemise+Homme"));
                annonce2.setSeller(vendeur);
                annonce2.setStatus(Annonce.Status.APPROVED);
                annonce2.setViewCount(78);
                annonce2.setContactCount(12);
                annonce2.setPublishedAt(LocalDateTime.now().minusDays(3));
                annonce2.setExpiresAt(LocalDateTime.now().plusDays(57));
                annonceRepository.save(annonce2);
                
                // Annonce 3: Sac à main
                Annonce annonce3 = new Annonce();
                annonce3.setTitle("Sac à main en cuir authentique");
                annonce3.setDescription("Superbe sac à main en cuir véritable, design moderne et élégant. Parfait pour toutes les occasions.");
                annonce3.setPrice(new BigDecimal("35000"));
                annonce3.setCategory(Annonce.Category.ACCESSOIRE);
                annonce3.setPublicationType(Annonce.PublicationType.STANDARD);
                annonce3.setCondition(Annonce.Condition.BON_ETAT);
                annonce3.setBrand("Local Artisan");
                annonce3.setColor("Marron");
                annonce3.setLocation("Thiès");
                annonce3.setImages(Arrays.asList("https://via.placeholder.com/400x400?text=Sac+a+Main"));
                annonce3.setSeller(vendeur);
                annonce3.setStatus(Annonce.Status.APPROVED);
                annonce3.setViewCount(32);
                annonce3.setContactCount(5);
                annonce3.setPublishedAt(LocalDateTime.now().minusDays(7));
                annonce3.setExpiresAt(LocalDateTime.now().plusDays(23));
                annonceRepository.save(annonce3);
                
                // Annonce 4: Promotion
                Annonce annonce4 = new Annonce();
                annonce4.setTitle("PROMOTION: Lot de 3 pagnes");
                annonce4.setDescription("Lot de 3 magnifiques pagnes en bazin, motifs variés. Prix promotionnel pour achat groupé.");
                annonce4.setPrice(new BigDecimal("18000"));
                annonce4.setCategory(Annonce.Category.PROMOTION);
                annonce4.setPublicationType(Annonce.PublicationType.TOP_PUB);
                annonce4.setCondition(Annonce.Condition.NEUF);
                annonce4.setBrand("Tissu Local");
                annonce4.setColor("Multicolore");
                annonce4.setLocation("Dakar");
                annonce4.setImages(Arrays.asList("https://via.placeholder.com/400x400?text=Lot+Pagnes"));
                annonce4.setSeller(vendeur);
                annonce4.setStatus(Annonce.Status.APPROVED);
                annonce4.setViewCount(156);
                annonce4.setContactCount(28);
                annonce4.setPublishedAt(LocalDateTime.now().minusDays(2));
                annonce4.setExpiresAt(LocalDateTime.now().plusDays(88));
                annonceRepository.save(annonce4);
                
                // Annonce 5: En attente d'approbation
                Annonce annonce5 = new Annonce();
                annonce5.setTitle("Baskets de sport neuves");
                annonce5.setDescription("Baskets de marque, jamais portées, taille 42. Parfaites pour le sport ou le quotidien.");
                annonce5.setPrice(new BigDecimal("12000"));
                annonce5.setCategory(Annonce.Category.HOMME);
                annonce5.setPublicationType(Annonce.PublicationType.STANDARD);
                annonce5.setCondition(Annonce.Condition.NEUF);
                annonce5.setSize("42");
                annonce5.setBrand("Nike");
                annonce5.setColor("Noir/Blanc");
                annonce5.setLocation("Dakar");
                annonce5.setImages(Arrays.asList("https://via.placeholder.com/400x400?text=Baskets"));
                annonce5.setSeller(vendeur);
                annonce5.setStatus(Annonce.Status.PENDING);
                annonce5.setViewCount(0);
                annonce5.setContactCount(0);
                annonceRepository.save(annonce5);
        }
    }
}
