package com.vendit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MessageCreateRequest {
    @NotNull
    private UUID conversationPublicId;
    @NotBlank
    private String content;
}
