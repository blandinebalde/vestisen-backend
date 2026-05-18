package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCheckoutDTO {
    private String checkoutId;
    private String clientSecret;
    private long amountDueFcfa;
    private long amountDueCents;
    private boolean stripeEnabled;
    private boolean demoMode;
    private String message;
}
