package com.vendit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscriptionCheckoutRequest {
    @NotBlank
    private String plan;
    private String billingCycle;
    @NotBlank
    private String idempotencyKey;
}
