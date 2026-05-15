package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private BigDecimal creditsPurchased;
    private BigDecimal creditsSpent;
    private List<StatsCreditsByMonthDTO> creditsByMonth;
    private List<StatsCreditsByYearDTO> creditsByYear;
    private List<StatsCreditsByUserDTO> creditsByUser;
    private List<StatsAnnoncesByMonthDTO> annoncesByMonth;
    private List<StatsAnnoncesByYearDTO> annoncesByYear;
    private List<StatsAnnoncesByCategoryDTO> annoncesByCategory;
    private List<StatsAnnoncesByStatusDTO> annoncesByStatus;
    private StatsEngagementDTO engagement;
}
