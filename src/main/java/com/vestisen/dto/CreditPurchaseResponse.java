package com.vestisen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditPurchaseResponse {
    private Long transactionId;
    /** Code unique de la transaction (18 caractères). */
    private String code;
    /** Pour Stripe : clientSecret à utiliser avec Stripe.js */
    private String clientSecret;
    private BigDecimal amountFcfa;
    private BigDecimal creditsAdded;
    private String paymentMethod;
}
