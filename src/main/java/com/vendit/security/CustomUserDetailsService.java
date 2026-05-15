package com.vendit.security;

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

import com.vendit.model.User;
import com.vendit.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service personnalisé pour charger les détails de l'utilisateur
 * Implémente UserDetailsService pour l'intégration avec Spring Security
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrivilegeSealService privilegeSealService;
    
    /**
     * Charge les détails de l'utilisateur par email ou téléphone (identifiant de connexion).
     * @param username email ou numéro de téléphone
     * @return UserDetails contenant les informations de l'utilisateur
     * @throws UsernameNotFoundException si l'utilisateur n'est pas trouvé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by email or phone: {}", username);
        
        User user = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> {
                    logger.warn("User not found with email/phone: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
        
        // Vérifier que l'utilisateur est activé
        if (!user.isEnabled()) {
            logger.warn("Attempted login with disabled account: {}", username);
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }

        if (!privilegeSealService.verifySeal(user)) {
            logger.warn("Privilege seal invalid for user id={} — possible direct database tampering", user.getId());
            throw new UsernameNotFoundException("User not found: " + username);
        }

        logger.debug("User loaded successfully: {} with role: {}", user.getEmail(), user.getRole());
        logger.debug("User enabled: {}, emailVerified: {}", user.isEnabled(), user.isEmailVerified());

        Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
        logger.debug("User authorities: {}", authorities);

        return AppUserDetails.fromUser(user, authorities);
    }
    
    /**
     * Rôle Spring ({@code ROLE_*}) + permissions métier ({@code perm:*}) pour {@code @PreAuthorize} / {@code hasAuthority}.
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        for (Permission p : RolePermissions.forRole(user.getRole())) {
            authorities.add(new SimpleGrantedAuthority(p.getAuthority()));
        }
        return authorities;
    }
}
