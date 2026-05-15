
package com.vendit.dto;

import com.vendit.model.User;

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
