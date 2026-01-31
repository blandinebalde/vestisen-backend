package com.vestisen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageCreateRequest {
    @NotNull
    private Long conversationId;
    @NotBlank
    private String content;
}
