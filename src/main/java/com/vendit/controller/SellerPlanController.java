package com.vendit.controller;

import com.vendit.dto.*;
import com.vendit.model.PlanBillingCycle;
import com.vendit.model.SellerPlan;
import com.vendit.model.User;
import com.vendit.repository.UserRepository;
import com.vendit.service.SellerPlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/seller/plan")
public class SellerPlanController {

    @Autowired
    private SellerPlanService sellerPlanService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/catalog")
    public ResponseEntity<List<SellerPlanCatalogItemDTO>> getCatalog() {
        return ResponseEntity.ok(sellerPlanService.getCatalog());
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @GetMapping("/status")
    public ResponseEntity<SellerSubscriptionStatusDTO> getStatus(Authentication authentication) {
        return ResponseEntity.ok(sellerPlanService.getSubscriptionStatus(currentUser(authentication)));
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @GetMapping("/commission-preview")
    public ResponseEntity<CommissionBreakdownDTO> previewCommission(
            @RequestParam BigDecimal amount,
            Authentication authentication) {
        return ResponseEntity.ok(sellerPlanService.previewCommission(amount, currentUser(authentication)));
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @PostMapping("/subscribe")
    public ResponseEntity<SellerSubscriptionStatusDTO> subscribe(
            @Valid @RequestBody SellerPlanSubscribeRequest request,
            Authentication authentication) {
        SellerPlan plan = parsePlan(request.getPlan());
        PlanBillingCycle cycle = parseBillingCycle(request.getBillingCycle());
        return ResponseEntity.ok(
                sellerPlanService.subscribe(currentUser(authentication), plan, cycle));
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @GetMapping("/commissions")
    public ResponseEntity<Page<SaleCommissionDTO>> listCommissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication authentication) {
        return ResponseEntity.ok(
                sellerPlanService.listCommissionsForSeller(currentUser(authentication), page, size));
    }

    private User currentUser(Authentication authentication) {
        UserDetails details = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private static SellerPlan parsePlan(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Plan requis");
        }
        return SellerPlan.valueOf(raw.trim().toUpperCase());
    }

    private static PlanBillingCycle parseBillingCycle(String raw) {
        if (raw == null || raw.isBlank()) {
            return PlanBillingCycle.MONTHLY;
        }
        return PlanBillingCycle.valueOf(raw.trim().toUpperCase());
    }
}
