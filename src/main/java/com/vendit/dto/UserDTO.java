package com.vendit.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.vendit.model.User;

@Data
public class UserDTO {
    private UUID publicId;
    private String code;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String whatsapp;
    private String avatarPath;
    private User.Role role;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int annoncesCount; // Nombre d'annonces de l'utilisateur
    private BigDecimal creditBalance; // Solde de crédits (pour publier des annonces)
    private String sellerPlan;
    private String sellerPlanLabel;
    private java.math.BigDecimal sellerCommissionPercent;
}
