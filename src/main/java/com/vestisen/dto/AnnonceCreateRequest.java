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
    private Annonce.Category category;
    
    @NotNull(message = "Publication type is required")
    private Annonce.PublicationType publicationType;
    
    private Annonce.Condition condition;
    private String size;
    private String brand;
    private String color;
    private String location;
    private List<String> images;
}
