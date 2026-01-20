package com.vestisen.controller;

import com.vestisen.dto.PaymentRequest;
import com.vestisen.model.Payment;
import com.vestisen.model.User;
import com.vestisen.repository.UserRepository;
import com.vestisen.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Payment payment = paymentService.createPayment(
            request.getAnnonceId(),
            user,
            request.getPaymentMethod()
        );
        
        return ResponseEntity.ok(payment);
    }
    
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Payment> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.confirmPayment(id));
    }
}
