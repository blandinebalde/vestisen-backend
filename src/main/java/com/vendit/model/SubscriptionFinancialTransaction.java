package com.vendit.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** Lignes financières append-only (montants en centimes). */
@Entity
@Table(name = "subscription_financial_transactions")
@Data
public class SubscriptionFinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SellerSubscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private SubscriptionFinancialType transactionType;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(nullable = false, length = 3)
    private String currency = "XOF";

    @Column(name = "stripe_invoice_id", length = 128)
    private String stripeInvoiceId;

    @Column(name = "stripe_payment_intent_id", length = 128)
    private String stripePaymentIntentId;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
