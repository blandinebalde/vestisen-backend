package com.vestisen.dto;

import com.vestisen.model.Annonce;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AnnonceCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
    @NotNull(message = "Category is required")
    private Long categoryId;
    
    @NotBlank(message = "Publication type (name) is required")
    private String publicationType;
    
    private Annonce.Condition condition;
    private String size;
    private String brand;
    private String color;
    private String location;
    private List<String> images;
    
    /** Option "tout doit partir" : prix réduits / lots */
    private Boolean toutDoitPartir;
    private BigDecimal originalPrice;
    private Boolean isLot;
    /** Paiement à la livraison accepté */
    private Boolean acceptPaymentOnDelivery;
    /** Géolocalisation */
    private Double latitude;
    private Double longitude;
}
