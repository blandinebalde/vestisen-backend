package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoriesOverviewDTO {
    private long totalCategories;
    private long activeCategories;
    private long inactiveCategories;
    private long totalAnnoncesInCategories;
    private List<StatsAnnoncesByCategoryDTO> topCategories;
}
