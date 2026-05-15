package com.vendit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.vendit.dto.PaymentRequest;
import com.vendit.dto.PaymentResponse;
import com.vendit.model.Payment;
import com.vendit.model.User;
import com.vendit.repository.UserRepository;
import com.vendit.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasAuthority('perm:payment:use')")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @jakarta.validation.Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Payment payment = paymentService.createPayment(
            request.getAnnoncePublicId(),
            user,
            request.getPaymentMethod()
        );
        
        return ResponseEntity.ok(paymentService.toPaymentResponse(payment));
    }
    
    @PostMapping("/{publicId}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable UUID publicId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Payment payment = paymentService.confirmPayment(publicId, user);
        return ResponseEntity.ok(paymentService.toPaymentResponse(payment));
    }
}
