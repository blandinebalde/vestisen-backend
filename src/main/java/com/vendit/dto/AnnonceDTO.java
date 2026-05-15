package com.vendit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.vendit.model.Annonce;

@Data
public class AnnonceDTO {
    /** Identifiant public (API, URLs). */
    private UUID publicId;
    private String code;
    private String title;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private String publicationType;
    private BigDecimal publicationCreditCost;
    private Annonce.Condition condition;
    private String size;
    private String brand;
    private String color;
    private String location;
    private List<String> images;
    private UUID sellerPublicId;
    private String sellerName;
    private String sellerPhone;
    private String sellerWhatsapp;
    private Annonce.Status status;
    private int viewCount;
    private int contactCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private boolean toutDoitPartir;
    private java.math.BigDecimal originalPrice;
    private boolean isLot;
    private boolean acceptPaymentOnDelivery;
    private Double latitude;
    private Double longitude;
}
