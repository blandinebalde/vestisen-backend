package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsAnnoncesByCategoryDTO {
    private Long categoryId;
    private String categoryName;
    private long count;
}
