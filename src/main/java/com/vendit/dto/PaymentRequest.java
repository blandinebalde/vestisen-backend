package com.vendit.dto;

import com.vendit.model.Payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentRequest {
    @NotNull(message = "Annonce public id is required")
    private UUID annoncePublicId;
    
    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;
}
