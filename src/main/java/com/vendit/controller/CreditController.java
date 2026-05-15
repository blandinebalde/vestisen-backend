package com.vendit.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.vendit.dto.CreditConfigDTO;
import com.vendit.dto.CreditLedgerEntryDTO;
import com.vendit.dto.CreditPurchaseRequest;
import com.vendit.dto.CreditPurchaseResponse;
import com.vendit.dto.CreditTransactionDTO;
import com.vendit.model.CreditConfig;
import com.vendit.model.CreditTransaction;
import com.vendit.model.User;
import com.vendit.repository.UserRepository;
import com.vendit.service.CreditService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

    @Autowired
    private CreditService creditService;

    @Autowired
    private UserRepository userRepository;

    /** Config publique : prix FCFA par crédit (pour afficher "1 crédit = X FCFA") */
    @GetMapping("/config")
    public ResponseEntity<CreditConfigDTO> getConfig() {
        CreditConfig config = creditService.getOrCreateConfig();
        CreditConfigDTO dto = new CreditConfigDTO();
        dto.setId(config.getId());
        dto.setPricePerCreditFcfa(config.getPricePerCreditFcfa());
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAuthority('perm:credit:balance_read')")
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(creditService.getBalance(user));
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @PostMapping("/purchase")
    public ResponseEntity<CreditPurchaseResponse> purchaseCredits(
            @Valid @RequestBody CreditPurchaseRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        CreditTransaction tx = creditService.purchaseCredits(
                user,
                request.getCredits(),
                request.getPaymentMethod()
        );
        CreditPurchaseResponse response = new CreditPurchaseResponse(
                tx.getPublicId(),
                tx.getCode(),
                tx.getTransactionId(), // clientSecret pour Stripe
                tx.getAmountFcfa(),
                tx.getCreditsAdded(),
                tx.getPaymentMethod().name()
        );
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @GetMapping("/ledger")
    public ResponseEntity<List<CreditLedgerEntryDTO>> getMyLedger(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(creditService.getLedgerForUserId(user.getId()));
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @GetMapping("/transactions")
    public ResponseEntity<List<CreditTransactionDTO>> getMyTransactions(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<CreditTransaction> list = creditService.getTransactionsByUserId(user.getId());
        List<CreditTransactionDTO> dtos = list.stream()
                .map(tx -> new CreditTransactionDTO(
                        tx.getPublicId(),
                        tx.getCode(),
                        tx.getAmountFcfa(),
                        tx.getCreditsAdded(),
                        tx.getPaymentMethod().name(),
                        tx.getStatus().name(),
                        tx.getCreatedAt(),
                        tx.getPaidAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('perm:credit:vendor')")
    @PostMapping("/confirm/{publicId}")
    public ResponseEntity<CreditTransactionDTO> confirmPurchase(
            @PathVariable("publicId") UUID transactionPublicId,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        CreditTransaction tx = creditService.confirmCreditPurchase(transactionPublicId, user.getId());
        CreditTransactionDTO dto = new CreditTransactionDTO(
                tx.getPublicId(),
                tx.getCode(),
                tx.getAmountFcfa(),
                tx.getCreditsAdded(),
                tx.getPaymentMethod().name(),
                tx.getStatus().name(),
                tx.getCreatedAt(),
                tx.getPaidAt());
        return ResponseEntity.ok(dto);
    }

    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
