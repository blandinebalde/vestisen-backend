package com.vestisen.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PublicationTarifDTO {
    private Long id;
    /** Nom du type de publication (saisi par l'admin dans le formulaire) */
    private String typeName;
    private BigDecimal price;
    /** Durée en jours ; null ou 0 = illimitée */
    private Integer durationDays;
    private boolean active;
}
