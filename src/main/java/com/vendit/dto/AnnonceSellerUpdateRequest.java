package com.vendit.dto;

import com.vendit.model.Annonce;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Mise à jour par le vendeur : champs optionnels ; statut / type de publication ignorés côté service. */
@Data
public class AnnonceSellerUpdateRequest {
    private String title;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private Annonce.Condition condition;
    private String size;
    private String brand;
    private String color;
    private String location;
    private List<String> images;
    private Boolean toutDoitPartir;
    private BigDecimal originalPrice;
    private Boolean isLot;
    private Boolean acceptPaymentOnDelivery;
    private Double latitude;
    private Double longitude;
}
