package com.vestisen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(unique = true)
    private String phone;
    
    private String address;
    
    private String whatsapp;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;
    
    private boolean enabled = true;
    
    @Column(nullable = false)
    private boolean emailVerified = false;
    
    @Column(name = "verification_token", length = 255)
    private String verificationToken;
    
    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;
    
    @Column(name = "reset_password_token", length = 255)
    private String resetPasswordToken;
    
    @Column(name = "reset_password_expiry")
    private LocalDateTime resetPasswordExpiry;
    
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Annonce> annonces = new HashSet<>();
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum Role {
        ADMIN, VENDEUR, USER
    }
}
