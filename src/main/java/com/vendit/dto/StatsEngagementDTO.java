package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsEngagementDTO {
    private long totalAnnonces;
    private long totalViews;
    private long totalContacts;
    private double avgViewsPerAnnonce;
    private double avgContactsPerAnnonce;
}
