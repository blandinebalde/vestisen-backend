package com.vestisen.dto;

import com.vestisen.model.Annonce;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AnnonceFilterRequest {
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String size;
    private String brand;
    private Annonce.Condition condition;
    private String search;
    private int page = 0;
    private int pageSize = 20;
    private String sortBy = "createdAt";
    private String sortDir = "DESC";
    /** Option "tout doit partir" */
    private Boolean toutDoitPartir;
    /** Géolocalisation : recherche proche de (lat, lng) dans radiusKm */
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
}
