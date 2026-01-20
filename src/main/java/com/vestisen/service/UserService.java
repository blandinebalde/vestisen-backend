package com.vestisen.service;

import com.vestisen.model.User;
import com.vestisen.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${admin.default.email}")
    private String adminEmail;
    
    @Value("${admin.default.password}")
    private String adminPassword;
    
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty() && userRepository.existsByPhone(user.getPhone())) {
            throw new RuntimeException("Phone number already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmailVerified(false);
        user.setEnabled(false); // Désactiver jusqu'à vérification email
        
        // Générer le token de vérification
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        
        User savedUser = userRepository.save(user);
        
        // Vérifier que le token a bien été sauvegardé
        logger.info("User created with ID: {}, Email: {}, Verification Token: {}, Expiry: {}", 
                   savedUser.getId(), savedUser.getEmail(), 
                   savedUser.getVerificationToken(), savedUser.getVerificationTokenExpiry());
        
        // Recharger depuis la base pour s'assurer que tout est bien sauvegardé
        User reloadedUser = userRepository.findById(savedUser.getId()).orElse(savedUser);
        logger.info("Reloaded user - Token: {}, Expiry: {}", 
                   reloadedUser.getVerificationToken(), reloadedUser.getVerificationTokenExpiry());
        
        // Envoyer l'email de vérification
        try {
            emailService.sendVerificationEmail(
                savedUser.getEmail(),
                verificationToken,
                savedUser.getFirstName()
            );
            logger.info("Verification email sent successfully to: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", savedUser.getEmail(), e);
            // Ne pas bloquer l'inscription si l'email échoue
        }
        
        return savedUser;
    }
    
    public boolean verifyEmail(String token) {
        logger.info("Attempting to verify email with token: {}", token);
        
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Verification token is null or empty");
            throw new RuntimeException("Token de vérification manquant");
        }
        
        String trimmedToken = token.trim();
        Optional<User> userOpt = userRepository.findByVerificationToken(trimmedToken);
        
        if (userOpt.isEmpty()) {
            logger.warn("No user found with verification token: {}", trimmedToken);
            // Log pour déboguer - vérifier combien d'utilisateurs non vérifiés existent
            long unverifiedCount = userRepository.findAll().stream()
                    .filter(u -> !u.isEmailVerified() && u.getVerificationToken() != null)
                    .count();
            logger.debug("Number of unverified users with tokens: {}", unverifiedCount);
            throw new RuntimeException("Token de vérification invalide ou introuvable. Le lien peut être incorrect ou avoir déjà été utilisé.");
        }
        
        User user = userOpt.get();
        logger.info("User found: {} with email: {}", user.getId(), user.getEmail());
        
        // Vérifier si l'email est déjà vérifié
        if (user.isEmailVerified()) {
            logger.info("Email {} is already verified", user.getEmail());
            throw new RuntimeException("Cet email a déjà été vérifié. Vous pouvez vous connecter.");
        }
        
        // Vérifier si le token a expiré
        if (user.getVerificationTokenExpiry() == null) {
            logger.warn("Verification token expiry is null for user: {}", user.getEmail());
            throw new RuntimeException("Le lien de vérification est invalide. Veuillez vous inscrire à nouveau.");
        }
        
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Verification token expired for user: {} at {}", user.getEmail(), user.getVerificationTokenExpiry());
            throw new RuntimeException("Le lien de vérification a expiré. Veuillez vous inscrire à nouveau.");
        }
        
        logger.info("Verifying email for user: {}", user.getEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        
        logger.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }
    
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        
        emailService.sendPasswordResetEmail(
            user.getEmail(),
            resetToken,
            user.getFirstName()
        );
    }
    
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));
        
        if (user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);
        userRepository.save(user);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public void initializeAdmin() {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFirstName("Admin");
            admin.setLastName("VestiSen");
            admin.setPhone("+221000000000");
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }
    }
}
