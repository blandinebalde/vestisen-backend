package com.vendit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ConversationDTO {
    private UUID publicId;
    private UUID annoncePublicId;
    private String annonceTitle;
    private UUID buyerPublicId;
    private String buyerName;
    private UUID sellerPublicId;
    private String sellerName;
    private LocalDateTime createdAt;
    private List<MessageDTO> messages;
}
