package com.vestisen.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditConfigDTO {
    private Long id;
    private BigDecimal pricePerCreditFcfa;
}
