package com.vendit.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_audit_logs")
@Data
public class SubscriptionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SellerSubscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private SubscriptionActorType actorType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_plan", length = 20)
    private SellerPlan previousPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_plan", length = 20)
    private SellerPlan newPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private SubscriptionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 20)
    private SubscriptionStatus newStatus;

    @Column(length = 512)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
