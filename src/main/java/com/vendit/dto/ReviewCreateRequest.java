package com.vendit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewCreateRequest {
    @NotNull
    private UUID annoncePublicId;
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;
    private String comment;
}
