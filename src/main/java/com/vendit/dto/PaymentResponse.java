package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID publicId;
    private UUID annoncePublicId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String transactionId;
    private String paymentProviderId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
