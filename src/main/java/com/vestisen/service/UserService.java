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

import java.security.SecureRandom;
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
        user.setCode(generateUniqueUserCode());
        
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

    /**
     * Création d'un utilisateur par l'admin : pas d'email de vérification, respecte enabled/emailVerified.
     */
    public User createUserByAdmin(User user, String plainPassword) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty() && userRepository.existsByPhone(user.getPhone())) {
            throw new RuntimeException("Un utilisateur avec ce numéro de téléphone existe déjà");
        }
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setCode(generateUniqueUserCode());
        // Ne pas envoyer d'email de vérification, ne pas forcer enabled/emailVerified
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        return userRepository.save(user);
    }

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateUniqueUserCode() {
        StringBuilder sb = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        String code = sb.toString();
        if (userRepository.existsByCode(code)) {
            return generateUniqueUserCode();
        }
        return code;
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
    
    public Optional<User> findByEmailOrPhone(String emailOrPhone) {
        return userRepository.findByEmailOrPhone(emailOrPhone);
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
            admin.setLastName("Vendit");
            admin.setPhone("+221000000000");
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            admin.setEmailVerified(true); // Admin email is automatically verified
            admin.setCode(generateUniqueUserCode());
            userRepository.save(admin);
            logger.info("Admin user initialized: {}", adminEmail);
        } else {
            // Mettre à jour l'admin existant pour s'assurer qu'il a les bonnes permissions
            User admin = userRepository.findByEmail(adminEmail).orElse(null);
            if (admin != null) {
                boolean needsUpdate = false;
                
                // Vérifier et corriger le rôle
                if (admin.getRole() != User.Role.ADMIN) {
                    admin.setRole(User.Role.ADMIN);
                    needsUpdate = true;
                }
                
                // Vérifier et corriger l'état activé
                if (!admin.isEnabled()) {
                    admin.setEnabled(true);
                    needsUpdate = true;
                }
                
                // Vérifier et corriger l'email vérifié
                if (!admin.isEmailVerified()) {
                    admin.setEmailVerified(true);
                    needsUpdate = true;
                }
                if (admin.getCode() == null || admin.getCode().isBlank()) {
                    admin.setCode(generateUniqueUserCode());
                    needsUpdate = true;
                }
                
                // Vérifier si le mot de passe est correctement encodé (commence par $2a$, $2b$ ou $2y$ pour BCrypt)
                String currentPassword = admin.getPassword();
                if (currentPassword == null || 
                    (!currentPassword.startsWith("$2a$") && 
                     !currentPassword.startsWith("$2b$") && 
                     !currentPassword.startsWith("$2y$"))) {
                    logger.warn("Admin password is not BCrypt encoded, re-encoding it");
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    needsUpdate = true;
                } else {
                    // Vérifier si le mot de passe actuel correspond au mot de passe par défaut
                    if (!passwordEncoder.matches(adminPassword, currentPassword)) {
                        logger.info("Admin password does not match default, updating it");
                        admin.setPassword(passwordEncoder.encode(adminPassword));
                        needsUpdate = true;
                    }
                }
                
                if (needsUpdate) {
                    userRepository.save(admin);
                    logger.info("Admin user updated: {} - Role: {}, Enabled: {}, EmailVerified: {}", 
                        adminEmail, admin.getRole(), admin.isEnabled(), admin.isEmailVerified());
                }
            }
        }
    }
}
