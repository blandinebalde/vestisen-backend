package com.vestisen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
