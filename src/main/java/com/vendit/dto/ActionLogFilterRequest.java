package com.vendit.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ActionLogFilterRequest {
    private String search;       // recherche basique (username, actionLabel, resourceType)
    private String username;
    private String userRole;
    private String resourceType;
    private String actionLabel;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Boolean success;
    private String httpMethod;
    private int page = 0;
    private int size = 20;

    public LocalDateTime getDateFromAsDateTime() {
        return dateFrom != null ? dateFrom.atStartOfDay() : null;
    }

    public LocalDateTime getDateToAsDateTime() {
        return dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;
    }
}
