package com.vendit.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.vendit.dto.ReviewCreateRequest;
import com.vendit.dto.ReviewDTO;
import com.vendit.model.User;
import com.vendit.repository.UserRepository;
import com.vendit.service.ReviewService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasAuthority('perm:review:write')")
    @PostMapping
    public ResponseEntity<ReviewDTO> create(@Valid @RequestBody ReviewCreateRequest request, Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(reviewService.create(request, user));
    }

    @PreAuthorize("hasAuthority('perm:review:read')")
    @GetMapping("/seller/{sellerPublicId}")
    public ResponseEntity<List<ReviewDTO>> getBySeller(@PathVariable UUID sellerPublicId,
                                                       @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        return ResponseEntity.ok(reviewService.findByRevieweePublicId(sellerPublicId, safeLimit));
    }

    private User getCurrentUser(Authentication auth) {
        UserDetails ud = (UserDetails) auth.getPrincipal();
        return userRepository.findByEmailOrPhone(ud.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
