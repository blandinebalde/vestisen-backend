package com.vendit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sale_commissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", length = 36, unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annonce_id", nullable = false, unique = true)
    private Annonce annonce;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "sale_amount_fcfa", nullable = false, precision = 14, scale = 2)
    private BigDecimal saleAmountFcfa;

    @Column(name = "commission_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercent;

    @Column(name = "commission_amount_fcfa", nullable = false, precision = 14, scale = 2)
    private BigDecimal commissionAmountFcfa;

    @Column(name = "seller_net_fcfa", nullable = false, precision = 14, scale = 2)
    private BigDecimal sellerNetFcfa;

    @Enumerated(EnumType.STRING)
    @Column(name = "seller_plan_at_sale", nullable = false, length = 20)
    private SellerPlan sellerPlanAtSale;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
