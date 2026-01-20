package com.vestisen.dto;

import com.vestisen.model.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull(message = "Annonce ID is required")
    private Long annonceId;
    
    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;
}
