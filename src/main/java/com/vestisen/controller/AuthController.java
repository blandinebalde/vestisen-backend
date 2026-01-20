package com.vestisen.controller;

import com.vestisen.dto.AuthRequest;
import com.vestisen.dto.AuthResponse;
import com.vestisen.dto.ErrorResponse;
import com.vestisen.dto.RegisterRequest;
import com.vestisen.dto.ResetPasswordRequest;
import com.vestisen.dto.SuccessResponse;
import com.vestisen.model.User;
import com.vestisen.security.JwtTokenProvider;
import com.vestisen.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        // Vérifier si l'email est vérifié (sauf pour les admins)
        if (!user.isEmailVerified() && user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.badRequest().body("Please verify your email before logging in. Check your inbox for the verification link.");
        }
        
        // Vérifier si le compte est activé
        if (!user.isEnabled()) {
            return ResponseEntity.badRequest().body("Your account is disabled. Please contact support.");
        }
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);
        
        return ResponseEntity.ok(new AuthResponse(token, user));
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
            userService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok().body(new SuccessResponse("Password reset link has been sent to your email."));
        } catch (RuntimeException e) {
            logger.warn("Password reset request failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
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
     * Endpoint de test pour vérifier les autorités de l'utilisateur connecté
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Not authenticated"));
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.findByEmail(userDetails.getUsername())
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("User not found"));
        }
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
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
     * Endpoint de test pour vérifier l'accès admin
     */
    @GetMapping("/test-admin")
    public ResponseEntity<?> testAdminAccess(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Not authenticated"));
        }
        
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("username", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(java.util.stream.Collectors.toList()));
        response.put("isAdmin", isAdmin);
        response.put("hasRoleAdmin", userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        
        if (isAdmin) {
            response.put("message", "Admin access granted");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Admin access denied");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }
}
