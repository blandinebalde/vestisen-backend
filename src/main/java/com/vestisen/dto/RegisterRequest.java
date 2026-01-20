package com.vestisen.dto;

import com.vestisen.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Account type is required")
    private String accountType; // "CLIENT" or "VENDEUR"
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    // Required for VENDEUR only
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Invalid phone number")
    private String phone;
    
    // Required for VENDEUR only
    private String address;
    
    // Required for VENDEUR only
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Invalid WhatsApp number")
    private String whatsapp;
    
    public User.Role getRole() {
        if ("VENDEUR".equalsIgnoreCase(accountType)) {
            return User.Role.VENDEUR;
        }
        return User.Role.USER;
    }
}
