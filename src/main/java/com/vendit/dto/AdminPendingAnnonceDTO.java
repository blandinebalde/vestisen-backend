package com.vendit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPendingAnnonceDTO {
    private UUID publicId;
    private String title;
    private String sellerEmail;
    private String categoryName;
    private String publicationType;
    private LocalDateTime createdAt;
}
