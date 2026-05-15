package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsAnnoncesByMonthDTO {
    private int year;
    private int month;
    private String label;
    private long created;
    private long approved;
    private long sold;
}
