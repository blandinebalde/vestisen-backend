package com.vendit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SellerPlanSubscribeRequest {
    @NotNull
    private String plan;
    /** MONTHLY ou ANNUAL ; ignoré pour FREE */
    private String billingCycle;
}
