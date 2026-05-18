package com.vendit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscriptionConfirmRequest {
    @NotBlank
    private String checkoutId;
    @NotBlank
    private String idempotencyKey;
}
