package com.vestisen.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditPurchaseRequest {
    /** Nombre de crédits à acheter */
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal credits;

    /** Méthode de paiement : STRIPE, WAVE, ORANGE_MONEY, CARD */
    @NotNull
    private String paymentMethod;
}
