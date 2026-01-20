package com.vestisen.security;

import com.vestisen.model.User;
import com.vestisen.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Service personnalisé pour charger les détails de l'utilisateur
 * Implémente UserDetailsService pour l'intégration avec Spring Security
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Charge les détails de l'utilisateur par email
     * @param email l'email de l'utilisateur
     * @return UserDetails contenant les informations de l'utilisateur
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });
        
        // Vérifier que l'utilisateur est activé
        if (!user.isEnabled()) {
            logger.warn("Attempted login with disabled account: {}", email);
            throw new UsernameNotFoundException("User account is disabled: " + email);
        }
        
        logger.debug("User loaded successfully: {} with role: {}", email, user.getRole());
        logger.debug("User enabled: {}, emailVerified: {}", user.isEnabled(), user.isEmailVerified());
        
        // Pour les admins, on peut permettre l'accès même si l'email n'est pas vérifié
        // (utile pour le compte admin initial)
        boolean accountEnabled = user.isEmailVerified() || user.getRole() == User.Role.ADMIN;
        
        Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
        logger.debug("User authorities: {}", authorities);
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),                    // accountNonExpired
                true,                               // accountNonLocked
                true,                               // credentialsNonExpired
                accountEnabled,                     // enabled (email vérifié ou admin)
                authorities
        );
    }
    
    /**
     * Récupère les autorités (rôles) de l'utilisateur
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }
}
