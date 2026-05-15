package com.vendit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.vendit.dto.AnnonceDTO;
import com.vendit.model.Annonce;
import com.vendit.model.CartItem;
import com.vendit.model.User;
import com.vendit.repository.AnnonceRepository;
import com.vendit.repository.CartItemRepository;
import com.vendit.repository.UserRepository;
import com.vendit.service.AnnonceService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Panier : clients et vendeurs authentifiés avec permission {@code perm:cart:use}. */
@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasAuthority('perm:cart:use')")
public class CartController {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnonceRepository annonceRepository;

    @Autowired
    private AnnonceService annonceService;

    @GetMapping
    public ResponseEntity<List<AnnonceDTO>> getMyCart(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<AnnonceDTO> dtos = items.stream()
                .map(item -> annonceService.getAnnonceDTOByPublicId(item.getAnnonce().getPublicId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/annonce/{annoncePublicId}")
    public ResponseEntity<Void> addToCart(@PathVariable UUID annoncePublicId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Annonce annonce = annonceRepository.findByPublicId(annoncePublicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        Long annonceInternalId = annonce.getId();
        if (cartItemRepository.existsByUserIdAndAnnonceId(user.getId(), annonceInternalId)) {
            return ResponseEntity.ok().build();
        }
        if (annonce.getSeller().getId().equals(user.getId())) {
            return ResponseEntity.badRequest().build();
        }
        if (annonce.getStatus() == Annonce.Status.SOLD) {
            return ResponseEntity.badRequest().build();
        }
        CartItem item = new CartItem();
        item.setUser(user);
        item.setAnnonce(annonce);
        cartItemRepository.save(item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/annonce/{annoncePublicId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable UUID annoncePublicId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        Annonce annonce = annonceRepository.findByPublicId(annoncePublicId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        cartItemRepository.findByUserIdAndAnnonceId(user.getId(), annonce.getId())
                .ifPresent(cartItemRepository::delete);
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
