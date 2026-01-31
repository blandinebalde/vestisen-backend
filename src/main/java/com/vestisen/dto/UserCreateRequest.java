package com.vestisen.dto;

import com.vestisen.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phone;
    private String address;
    private String whatsapp;

    @NotNull
    private User.Role role = User.Role.USER;

    private Boolean enabled = true;
    private Boolean emailVerified = false;
}
