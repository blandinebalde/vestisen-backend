package com.vestisen.dto;

import com.vestisen.model.Annonce;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PublicationTarifDTO {
    private Long id;
    private Annonce.PublicationType publicationType;
    private BigDecimal price;
    private int durationDays;
    private boolean active;
}
