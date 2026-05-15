package com.vendit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageDTO {
    private Long id;
    private UUID conversationPublicId;
    private UUID senderPublicId;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
