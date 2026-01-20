package com.vestisen.dto;

import com.vestisen.model.Annonce;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnonceDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private Annonce.Category category;
    private Annonce.PublicationType publicationType;
    private Annonce.Condition condition;
    private String size;
    private String brand;
    private String color;
    private String location;
    private List<String> images;
    private Long sellerId;
    private String sellerName;
    private String sellerPhone;
    private Annonce.Status status;
    private int viewCount;
    private int contactCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
}
