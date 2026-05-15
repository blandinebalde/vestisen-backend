package com.vendit.dto;

import com.vendit.model.User;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    /** Durée de vie du jeton d'accès en secondes (alignée sur la config serveur). */
    private Long expiresInSeconds;
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
    private BigDecimal creditBalance;
    private String sellerPlan;
    private String sellerPlanLabel;

    public AuthResponse(String token, User user, long expiresInSeconds) {
        this.token = token;
        this.expiresInSeconds = expiresInSeconds;
        this.publicId = user.getPublicId();
        this.code = user.getCode();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = user.getPhone();
        this.address = user.getAddress();
        this.whatsapp = user.getWhatsapp();
        this.avatarPath = user.getAvatarPath();
        this.role = user.getRole();
        this.creditBalance = user.getCreditBalance() != null ? user.getCreditBalance() : BigDecimal.ZERO;
        if (user.getSellerPlan() != null) {
            this.sellerPlan = user.getSellerPlan().name();
            this.sellerPlanLabel = com.vendit.model.SellerPlanCatalog.get(user.getSellerPlan()).getLabel();
        }
    }
}
