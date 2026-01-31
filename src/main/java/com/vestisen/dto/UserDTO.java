package com.vestisen.dto;

import com.vestisen.model.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String code;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String whatsapp;
    private User.Role role;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int annoncesCount; // Nombre d'annonces de l'utilisateur
    private BigDecimal creditBalance; // Solde de crédits (pour publier des annonces)
}
