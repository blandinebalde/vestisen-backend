package com.vestisen.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationDTO {
    private Long id;
    private Long annonceId;
    private String annonceTitle;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private LocalDateTime createdAt;
    private List<MessageDTO> messages;
}
