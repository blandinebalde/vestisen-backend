package com.vendit.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.vendit.dto.AuthRequest;
import com.vendit.dto.AuthResponse;
import com.vendit.dto.ErrorResponse;
import com.vendit.dto.ProfileUpdateRequest;
import com.vendit.dto.RegisterRequest;
import com.vendit.dto.ResetPasswordRequest;
import com.vendit.dto.SuccessResponse;
import com.vendit.model.User;
import com.vendit.security.JwtTokenProvider;
import com.vendit.service.FileStorageService;
import com.vendit.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")

public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            User user = userService.findByEmailOrPhone(request.getEmailOrPhone())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            if (!user.isEmailVerified() && user.getRole() != User.Role.ADMIN) {
                return ResponseEntity.badRequest().body("Please verify your email before logging in. Check your inbox for the verification link.");
            }
            if (!user.isEnabled()) {
                return ResponseEntity.badRequest().body("Your account is disabled. Please contact support.");
            }
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmailOrPhone(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new AuthResponse(token, user, tokenProvider.getAccessTokenValiditySeconds()));
        } catch (BadCredentialsException e) {
            logger.warn("Login failed for: {}", request.getEmailOrPhone());
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid email/phone or password"));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Validation selon le type de compte
        if ("VENDEUR".equalsIgnoreCase(request.getAccountType())) {
            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                return ResponseEntity.badRequest().body("Phone number is required for vendor account");
            }
            if (request.getAddress() == null || request.getAddress().isEmpty()) {
                return ResponseEntity.badRequest().body("Address is required for vendor account");
            }
            if (request.getWhatsapp() == null || request.getWhatsapp().isEmpty()) {
                return ResponseEntity.badRequest().body("WhatsApp number is required for vendor account");
            }
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        
        // Champs spécifiques au vendeur
        if ("VENDEUR".equalsIgnoreCase(request.getAccountType())) {
            user.setPhone(request.getPhone());
            user.setAddress(request.getAddress());
            user.setWhatsapp(request.getWhatsapp());
        }
        
        try {
            userService.createUser(user);
            
            // Ne pas connecter automatiquement, l'utilisateur doit vérifier son email
            return ResponseEntity.ok().body(new SuccessResponse("Registration successful. Please check your email to verify your account."));
        } catch (RuntimeException e) {
            // Gérer les erreurs de création d'utilisateur (email existant, etc.)
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Gérer les autres erreurs (envoi d'email, etc.)
            logger.error("Error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred during registration. Please try again later."));
        }
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            userService.verifyEmail(token);
            return ResponseEntity.ok().body(new SuccessResponse("Email verified successfully. You can now login."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody AuthRequest request) {
        try {
            userService.findByEmailOrPhone(request.getEmailOrPhone()).ifPresent(user ->
                userService.requestPasswordReset(user.getEmail()));
            return ResponseEntity.ok().body(new SuccessResponse("If this email is registered, a password reset link has been sent."));
        } catch (Exception e) {
            logger.error("Error during password reset request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred. Please try again later."));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok().body(new SuccessResponse("Password has been reset successfully."));
        } catch (RuntimeException e) {
            logger.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during password reset: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred. Please try again later."));
        }
    }

    /**
     * Révoque le jeton d'accès courant (liste noire côté serveur jusqu'à expiration) et vide le contexte de sécurité.
     */
    @PreAuthorize("hasAuthority('perm:profile:access')")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            try {
                tokenProvider.revokeAccessToken(authHeader.substring(7));
            } catch (Exception e) {
                logger.debug("Logout: impossible de révoquer le jeton ({})", e.getMessage());
            }
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Endpoint de test pour vérifier les autorités de l'utilisateur connecté
     */
    @PreAuthorize("hasAuthority('perm:profile:access')")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Not authenticated"));
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByEmailOrPhone(userDetails.getUsername())
                .orElse(userService.findByEmail(userDetails.getUsername()).orElse(null));
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("User not found"));
        }
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("publicId", user.getPublicId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());
        response.put("enabled", user.isEnabled());
        response.put("emailVerified", user.isEmailVerified());
        response.put("authorities", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(java.util.stream.Collectors.toList()));
        response.put("authenticated", authentication.isAuthenticated());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour le profil de l'utilisateur connecté (nom, prénom, téléphone, adresse, whatsapp).
     */
    @PreAuthorize("hasAuthority('perm:profile:access')")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Non authentifié"));
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByEmailOrPhone(userDetails.getUsername())
                .orElse(userService.findByEmail(userDetails.getUsername()).orElse(null));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Utilisateur introuvable"));
        }
        try {
            User updated = userService.updateProfile(user.getId(), request);
            return ResponseEntity.ok(userToMap(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Upload de la photo de profil. Accepte un fichier multipart avec la clé "file" ou "files".
     */
    @PreAuthorize("hasAuthority('perm:profile:access')")
    @PostMapping("/profile/photo")
    public ResponseEntity<?> uploadProfilePhoto(
            Authentication authentication,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Non authentifié"));
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByEmailOrPhone(userDetails.getUsername())
                .orElse(userService.findByEmail(userDetails.getUsername()).orElse(null));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Utilisateur introuvable"));
        }
        MultipartFile toUpload = file;
        if ((toUpload == null || toUpload.isEmpty()) && files != null && files.length > 0) {
            toUpload = files[0];
        }
        if (toUpload == null || toUpload.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Aucun fichier fourni"));
        }
        try {
            String path = fileStorageService.storeProfilePhoto(user.getCode(), toUpload);
            if (path == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Fichier invalide (type ou taille). Utilisez une image JPG/PNG/WebP max 2 Mo."));
            }
            User updated = userService.setAvatarPath(user.getId(), path);
            return ResponseEntity.ok(userToMap(updated));
        } catch (Exception e) {
            logger.error("Profile photo upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erreur lors de l'upload de la photo."));
        }
    }

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("publicId", u.getPublicId());
        m.put("code", u.getCode());
        m.put("email", u.getEmail());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("phone", u.getPhone());
        m.put("address", u.getAddress());
        m.put("whatsapp", u.getWhatsapp());
        m.put("avatarPath", u.getAvatarPath());
        m.put("role", u.getRole() != null ? u.getRole().name() : "USER");
        m.put("enabled", u.isEnabled());
        m.put("creditBalance", u.getCreditBalance() != null ? u.getCreditBalance().doubleValue() : 0.0);
        return m;
    }
    
}
