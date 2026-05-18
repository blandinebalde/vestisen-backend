package com.vendit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScheduleDowngradeRequest {
    @NotBlank
    private String plan;
    private Long expectedVersion;
}
