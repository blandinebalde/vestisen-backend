package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsAnnoncesByYearDTO {
    private int year;
    private long created;
    private long approved;
    private long sold;
}
