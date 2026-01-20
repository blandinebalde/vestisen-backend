package com.vestisen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    
    private String description;
    private String icon;
    private Boolean active;
}
