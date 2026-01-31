package com.vestisen.controller;

import com.vestisen.dto.AnnonceDTO;
import com.vestisen.model.Annonce;
import com.vestisen.model.CartItem;
import com.vestisen.model.User;
import com.vestisen.repository.CartItemRepository;
import com.vestisen.repository.UserRepository;
import com.vestisen.repository.AnnonceRepository;
import com.vestisen.service.AnnonceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** Panier : réservé aux clients (USER). */
@RestController
@RequestMapping("/api/cart")
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
                .map(item -> annonceService.getAnnonceDTOById(item.getAnnonce().getId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/annonce/{annonceId}")
    public ResponseEntity<Void> addToCart(@PathVariable Long annonceId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (cartItemRepository.existsByUserIdAndAnnonceId(user.getId(), annonceId)) {
            return ResponseEntity.ok().build();
        }
        Annonce annonce = annonceRepository.findById(annonceId)
                .orElseThrow(() -> new RuntimeException("Annonce not found"));
        CartItem item = new CartItem();
        item.setUser(user);
        item.setAnnonce(annonce);
        cartItemRepository.save(item);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/annonce/{annonceId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long annonceId, Authentication authentication) {
        User user = getCurrentUser(authentication);
        cartItemRepository.findByUserIdAndAnnonceId(user.getId(), annonceId)
                .ifPresent(cartItemRepository::delete);
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
