package com.vestisen.controller;

import com.vestisen.dto.ReviewCreateRequest;
import com.vestisen.dto.ReviewDTO;
import com.vestisen.model.User;
import com.vestisen.repository.UserRepository;
import com.vestisen.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ReviewDTO> create(@Valid @RequestBody ReviewCreateRequest request, Authentication auth) {
        User user = getCurrentUser(auth);
        return ResponseEntity.ok(reviewService.create(request, user));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ReviewDTO>> getBySeller(@PathVariable Long sellerId,
                                                       @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 100);
        return ResponseEntity.ok(reviewService.findByRevieweeId(sellerId, safeLimit));
    }

    private User getCurrentUser(Authentication auth) {
        UserDetails ud = (UserDetails) auth.getPrincipal();
        return userRepository.findByEmailOrPhone(ud.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
