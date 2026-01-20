package com.vestisen.controller;

import com.vestisen.config.GoogleAdsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {
    
    @Autowired
    private GoogleAdsConfig googleAdsConfig;
    
    @GetMapping("/google-ads")
    public ResponseEntity<Map<String, Object>> getGoogleAdsConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", googleAdsConfig.isEnabled());
        config.put("clientId", googleAdsConfig.getClientId());
        
        if (googleAdsConfig.isEnabled() && googleAdsConfig.getAdSlots() != null) {
            Map<String, String> adSlots = new HashMap<>();
            adSlots.put("homeHeader", googleAdsConfig.getAdSlots().getHomeHeader());
            adSlots.put("homeSidebar", googleAdsConfig.getAdSlots().getHomeSidebar());
            adSlots.put("catalogueTop", googleAdsConfig.getAdSlots().getCatalogueTop());
            adSlots.put("catalogueBottom", googleAdsConfig.getAdSlots().getCatalogueBottom());
            adSlots.put("productSidebar", googleAdsConfig.getAdSlots().getProductSidebar());
            config.put("adSlots", adSlots);
        }
        
        return ResponseEntity.ok(config);
    }
}
