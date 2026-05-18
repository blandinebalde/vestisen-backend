package com.vendit.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_subscriptions")
@Data
public class SellerSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private SellerPlan planType = SellerPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "renewal_date")
    private LocalDateTime renewalDate;

    @Column(name = "stripe_subscription_id", length = 128)
    private String stripeSubscriptionId;

    @Column(name = "stripe_customer_id", length = 128)
    private String stripeCustomerId;

    @Column(name = "boosts_remaining", nullable = false)
    private int boostsRemaining;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate = new BigDecimal("15");

    @Enumerated(EnumType.STRING)
    @Column(name = "scheduled_downgrade", length = 20)
    private SellerPlan scheduledDowngrade;

    @Column(name = "downgrade_locked", nullable = false)
    private boolean downgradeLocked;

    @Column(name = "grace_until")
    private LocalDateTime graceUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", length = 20)
    private PlanBillingCycle billingCycle;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
