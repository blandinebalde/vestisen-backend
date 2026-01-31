package com.vestisen.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private Long id;
    private Long annonceId;
    private Long reviewerId;
    private String reviewerName;
    private Long revieweeId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
