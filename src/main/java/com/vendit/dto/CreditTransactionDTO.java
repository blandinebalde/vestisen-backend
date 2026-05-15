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
public class CreditTransactionDTO {
    private UUID publicId;
    private String code;
    private BigDecimal amountFcfa;
    private BigDecimal creditsAdded;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
