package com.vestisen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditTransactionDTO {
    private Long id;
    private String code;
    private BigDecimal amountFcfa;
    private BigDecimal creditsAdded;
    private String paymentMethod;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
