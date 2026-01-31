package com.vestisen.controller;

import com.vestisen.dto.CreditConfigDTO;
import com.vestisen.dto.CreditPurchaseRequest;
import com.vestisen.dto.CreditPurchaseResponse;
import com.vestisen.dto.CreditTransactionDTO;
import com.vestisen.model.CreditConfig;
import com.vestisen.model.CreditTransaction;
import com.vestisen.model.User;
import com.vestisen.repository.UserRepository;
import com.vestisen.service.CreditService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
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

    /** Solde de crédits de l'utilisateur connecté */
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(creditService.getBalance(user));
    }

    /** Initier un achat de crédits (carte, Wave, etc.) — réservé aux vendeurs et admins. */
    @PostMapping("/purchase")
    public ResponseEntity<CreditPurchaseResponse> purchaseCredits(
            @Valid @RequestBody CreditPurchaseRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user.getRole() != User.Role.VENDEUR && user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }
        CreditTransaction tx = creditService.purchaseCredits(
                user,
                request.getCredits(),
                request.getPaymentMethod()
        );
        CreditPurchaseResponse response = new CreditPurchaseResponse(
                tx.getId(),
                tx.getCode(),
                tx.getTransactionId(), // clientSecret pour Stripe
                tx.getAmountFcfa(),
                tx.getCreditsAdded(),
                tx.getPaymentMethod().name()
        );
        return ResponseEntity.ok(response);
    }

    /** Historique des achats de crédits de l'utilisateur connecté (vendeur/admin). */
    @GetMapping("/transactions")
    public ResponseEntity<List<CreditTransactionDTO>> getMyTransactions(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<CreditTransaction> list = creditService.getTransactionsByUserId(user.getId());
        List<CreditTransactionDTO> dtos = list.stream()
                .map(tx -> new CreditTransactionDTO(
                        tx.getId(),
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

    /** Confirmer un achat après paiement réussi (appelé par le front après Stripe success, ou par webhook) */
    @PostMapping("/confirm/{id}")
    public ResponseEntity<CreditTransaction> confirmPurchase(
            @PathVariable("id") Long transactionId,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        CreditTransaction tx = creditService.findTransactionById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!tx.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(creditService.confirmCreditPurchase(transactionId));
    }

    private User getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
