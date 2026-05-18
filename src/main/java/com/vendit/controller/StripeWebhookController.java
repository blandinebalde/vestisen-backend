package com.vendit.controller;

import com.vendit.service.SellerSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    @Autowired
    private SellerSubscriptionService sellerSubscriptionService;

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().body("ignored");
        }
        boolean handled = sellerSubscriptionService.handleStripeWebhook(payload, signature);
        return handled ? ResponseEntity.ok("ok") : ResponseEntity.badRequest().body("ignored");
    }
}
