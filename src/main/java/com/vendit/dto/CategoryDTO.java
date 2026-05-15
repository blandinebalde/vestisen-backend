package com.vendit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private boolean active;
    private long annoncesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
