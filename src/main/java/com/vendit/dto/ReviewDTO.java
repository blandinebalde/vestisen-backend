
package com.vendit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewDTO {
    private Long id;
    private UUID annoncePublicId;
    private UUID reviewerPublicId;
    private String reviewerName;
    private UUID revieweePublicId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
