package com.vestisen.controller;

import com.vestisen.dto.AnnonceCreateRequest;
import com.vestisen.dto.AnnonceDTO;
import com.vestisen.dto.AnnonceFilterRequest;
import com.vestisen.model.Annonce;
import com.vestisen.model.User;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.repository.UserRepository;
import com.vestisen.service.AnnonceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/annonces")
public class AnnonceController {
    
    @Autowired
    private AnnonceService annonceService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AnnonceRepository annonceRepository;
    
    @GetMapping("/public")
    public ResponseEntity<Page<AnnonceDTO>> getPublicAnnonces(AnnonceFilterRequest filter) {
        return ResponseEntity.ok(annonceService.searchAnnonces(filter));
    }
    
    @GetMapping("/public/{id}")
    public ResponseEntity<AnnonceDTO> getPublicAnnonce(@PathVariable Long id) {
        return ResponseEntity.ok(annonceService.getAnnonceById(id));
    }
    
    @GetMapping("/public/top")
    public ResponseEntity<List<AnnonceDTO>> getTopAnnonces(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "10") int limit) {
        if (type != null && !type.isBlank()) {
            return ResponseEntity.ok(annonceService.getTopAnnonces(type.trim(), limit));
        }
        return ResponseEntity.ok(annonceService.getTopViewedAnnonces(limit));
    }
    
    @PostMapping("/contact/{id}")
    public ResponseEntity<Void> contactSeller(@PathVariable Long id) {
        annonceService.incrementContactCount(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping
    public ResponseEntity<AnnonceDTO> createAnnonce(
            @Valid @RequestBody AnnonceCreateRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Seuls les vendeurs et administrateurs peuvent publier des annonces
        if (user.getRole() != User.Role.VENDEUR && user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(annonceService.createAnnonce(request, user));
    }
    
    @GetMapping("/my-annonces")
    public ResponseEntity<Page<AnnonceDTO>> getMyAnnonces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Annonce> userAnnonces = annonceRepository.findBySellerId(user.getId(), pageable);
        
        return ResponseEntity.ok(userAnnonces.map(annonceService::toDTO));
    }

    /** Historique d'achats du client (annonces qu'il a achetées, statut SOLD). */
    @GetMapping("/my-purchases")
    public ResponseEntity<List<AnnonceDTO>> getMyPurchases(Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.getMyPurchases(user.getId()));
    }

    /** Confirmer l'achat d'une annonce (client) : marque l'annonce comme vendue, définit l'acheteur, retire du panier. */
    @PostMapping("/{id}/buy")
    public ResponseEntity<AnnonceDTO> buyAnnonce(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.buyAnnonce(id, user));
    }

    /** Upload des photos pour une annonce (stockage dans annonce/user/{codeAnnonce}/). */
    @PostMapping("/{id}/photos")
    public ResponseEntity<AnnonceDTO> uploadPhotos(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            return ResponseEntity.ok(annonceService.addPhotos(id, user, files));
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
