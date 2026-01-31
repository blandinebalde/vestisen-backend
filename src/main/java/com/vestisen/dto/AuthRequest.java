package com.vestisen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    /** Email ou numéro de téléphone pour la connexion */
    @NotBlank(message = "Email or phone is required")
    private String emailOrPhone;
    
    @NotBlank(message = "Password is required")
    private String password;
}
