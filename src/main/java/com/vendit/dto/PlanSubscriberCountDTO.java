package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanSubscriberCountDTO {
    private String plan;
    private String label;
    private long count;
}
