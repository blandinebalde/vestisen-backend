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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/annonces")
@CrossOrigin(origins = "*")
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
            @RequestParam(required = false) Annonce.PublicationType type,
            @RequestParam(defaultValue = "10") int limit) {
        if (type != null) {
            return ResponseEntity.ok(annonceService.getTopAnnonces(type, limit));
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
        
        Pageable pageable = PageRequest.of(page, size);
        // Retourner toutes les annonces de l'utilisateur (tous statuts)
        Page<Annonce> userAnnonces = annonceRepository.findBySellerId(user.getId(), pageable);
        
        return ResponseEntity.ok(userAnnonces.map(annonceService::toDTO));
    }
}
