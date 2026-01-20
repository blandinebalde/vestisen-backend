package com.vestisen.dto;

import com.vestisen.model.User;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String whatsapp;
    private User.Role role;
    private Boolean enabled;
    private Boolean emailVerified;
}
