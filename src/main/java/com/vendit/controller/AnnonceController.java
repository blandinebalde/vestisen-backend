package com.vendit.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vendit.dto.AnnonceCreateRequest;
import com.vendit.dto.AnnonceDTO;
import com.vendit.dto.AnnonceFilterRequest;
import com.vendit.dto.AnnonceSellerUpdateRequest;
import com.vendit.dto.AnnonceValidationResponseDTO;
import com.vendit.dto.MyAnnoncesSummaryDTO;
import com.vendit.model.User;
import com.vendit.repository.UserRepository;
import com.vendit.service.AnnonceCreateValidationService;
import com.vendit.service.AnnonceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/annonces")
public class AnnonceController {
    
    @Autowired
    private AnnonceService annonceService;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnonceCreateValidationService annonceCreateValidationService;

    @GetMapping("/public")
    public ResponseEntity<Page<AnnonceDTO>> getPublicAnnonces(AnnonceFilterRequest filter) {
        return ResponseEntity.ok(annonceService.searchAnnonces(filter));
    }
    
    @GetMapping("/public/{publicId}")
    public ResponseEntity<AnnonceDTO> getPublicAnnonce(@PathVariable UUID publicId) {
        return ResponseEntity.ok(annonceService.getAnnonceByPublicId(publicId));
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
    
    @PreAuthorize("hasAuthority('perm:market:contact')")
    @PostMapping("/contact/{publicId}")
    public ResponseEntity<Void> contactSeller(@PathVariable UUID publicId) {
        annonceService.incrementContactCount(publicId);
        return ResponseEntity.ok().build();
    }
    
    @PreAuthorize("hasAuthority('perm:annonce:create')")
    @PostMapping("/validate/details")
    public ResponseEntity<AnnonceValidationResponseDTO> validateCreateDetails(
            @RequestBody AnnonceCreateRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(annonceCreateValidationService.validateDetails(request, user));
    }

    @PreAuthorize("hasAuthority('perm:annonce:create')")
    @PostMapping("/validate/visibility")
    public ResponseEntity<AnnonceValidationResponseDTO> validateCreateVisibility(
            @RequestBody AnnonceCreateRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(annonceCreateValidationService.validateVisibility(request, user));
    }

    @PreAuthorize("hasAuthority('perm:annonce:create')")
    @PostMapping(value = "/validate/photos", consumes = "multipart/form-data")
    public ResponseEntity<AnnonceValidationResponseDTO> validateCreatePhotos(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(annonceCreateValidationService.validatePhotos(files, user));
    }

    @PreAuthorize("hasAuthority('perm:annonce:create')")
    @PostMapping("/validate/confirm")
    public ResponseEntity<AnnonceValidationResponseDTO> validateCreateConfirm(
            @RequestBody AnnonceCreateRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(annonceCreateValidationService.validateConfirm(request, user));
    }

    @PreAuthorize("hasAuthority('perm:annonce:create')")
    @PostMapping
    public ResponseEntity<AnnonceDTO> createAnnonce(
            @Valid @RequestBody AnnonceCreateRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        return ResponseEntity.ok(annonceService.createAnnonce(request, user));
    }

    private User resolveUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @PreAuthorize("hasAuthority('perm:annonce:seller_read')")
    @GetMapping("/my-annonces")
    public ResponseEntity<Page<AnnonceDTO>> getMyAnnonces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.listMyAnnonces(user, page, size, status, search));
    }

    @PreAuthorize("hasAuthority('perm:annonce:seller_read')")
    @GetMapping("/my-annonces/summary")
    public ResponseEntity<MyAnnoncesSummaryDTO> getMyAnnoncesSummary(Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.getMyAnnoncesSummary(user));
    }

    @PreAuthorize("hasAuthority('perm:annonce:seller_read')")
    @GetMapping("/mine/{publicId}")
    public ResponseEntity<AnnonceDTO> getMyAnnonce(
            @PathVariable UUID publicId,
            Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.getMyAnnonceForSeller(publicId, user));
    }

    @PreAuthorize("hasAuthority('perm:annonce:seller_write')")
    @PutMapping("/mine/{publicId}")
    public ResponseEntity<AnnonceDTO> updateMyAnnonce(
            @PathVariable UUID publicId,
            @RequestBody AnnonceSellerUpdateRequest request,
            Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.updateMyAnnonceForSeller(publicId, user, request));
    }

    @PreAuthorize("hasAuthority('perm:annonce:seller_write')")
    @DeleteMapping("/mine/{publicId}")
    public ResponseEntity<Void> deleteMyAnnonce(
            @PathVariable UUID publicId,
            Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        annonceService.deleteMyAnnonceForSeller(publicId, user);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('perm:market:buy')")
    @GetMapping("/my-purchases")
    public ResponseEntity<List<AnnonceDTO>> getMyPurchases(Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.getMyPurchases(user.getId()));
    }

    @PreAuthorize("hasAuthority('perm:market:buy')")
    @PostMapping("/{publicId}/buy")
    public ResponseEntity<AnnonceDTO> buyAnnonce(@PathVariable UUID publicId, Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(annonceService.buyAnnonce(publicId, user));
    }

    @PreAuthorize("hasAuthority('perm:annonce:seller_read')")
    @PostMapping("/{publicId}/photos")
    public ResponseEntity<?> uploadPhotos(
            @PathVariable UUID publicId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {
        User user = userRepository.findByEmail(((UserDetails) authentication.getPrincipal()).getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Aucune photo fournie. Envoyez au moins un fichier avec le paramètre 'files'."));
        }
        try {
            return ResponseEntity.ok(annonceService.addPhotos(publicId, user, files));
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Erreur lors de l'enregistrement des photos: " + e.getMessage()));
        }
    }
}
