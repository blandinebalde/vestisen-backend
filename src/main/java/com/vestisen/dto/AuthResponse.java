package com.vestisen.dto;

import com.vestisen.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String code;
    private String email;
    private String firstName;
    private String lastName;
    private User.Role role;
    private BigDecimal creditBalance;
    
    public AuthResponse(String token, User user) {
        this.token = token;
        this.id = user.getId();
        this.code = user.getCode();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.role = user.getRole();
        this.creditBalance = user.getCreditBalance() != null ? user.getCreditBalance() : BigDecimal.ZERO;
    }
}
