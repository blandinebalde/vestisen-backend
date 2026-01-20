package com.vestisen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.ads")
@Data
public class GoogleAdsConfig {
    
    /**
     * ID du client Google Ads (ex: ca-pub-xxxxxxxxxxxxxxxx)
     */
    private String clientId;
    
    /**
     * Activer ou désactiver Google Ads
     */
    private boolean enabled = false;
    
    /**
     * Emplacements des annonces
     */
    private AdSlots adSlots = new AdSlots();
    
    @Data
    public static class AdSlots {
        /**
         * Emplacement pour la page d'accueil (header)
         */
        private String homeHeader;
        
        /**
         * Emplacement pour la page d'accueil (sidebar)
         */
        private String homeSidebar;
        
        /**
         * Emplacement pour le catalogue (top)
         */
        private String catalogueTop;
        
        /**
         * Emplacement pour le catalogue (bottom)
         */
        private String catalogueBottom;
        
        /**
         * Emplacement pour la page de détail produit (sidebar)
         */
        private String productSidebar;
    }
}
