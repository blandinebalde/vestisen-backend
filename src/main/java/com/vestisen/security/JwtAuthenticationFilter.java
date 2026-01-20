package com.vestisen.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT pour l'authentification des requêtes
 * Extrait et valide le token JWT depuis le header Authorization
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // Log pour toutes les requêtes vers /api/admin pour déboguer
        if (requestPath.startsWith("/api/admin") || requestPath.startsWith("/api/auth")) {
            logger.info("JWT Filter - Processing request to: {}", requestPath);
            logger.info("JWT Filter - Authorization header present: {}", authHeader != null);
            if (authHeader != null) {
                logger.info("JWT Filter - Authorization header starts with Bearer: {}", authHeader.startsWith(BEARER_PREFIX));
                logger.info("JWT Filter - Authorization header length: {}", authHeader.length());
            }
        }
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                if (requestPath.startsWith("/api/admin") || requestPath.startsWith("/api/auth")) {
                    logger.info("JWT Filter - JWT token extracted, length: {}", jwt.length());
                }
                
                if (tokenProvider.validateToken(jwt)) {
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    
                    if (requestPath.startsWith("/api/admin") || requestPath.startsWith("/api/auth")) {
                        logger.info("JWT Filter - Token validated, username: {}", username);
                    }
                    
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        // Vérifier que l'utilisateur est activé
                        if (!userDetails.isEnabled()) {
                            logger.warn("Attempted authentication with disabled user: {} on path: {}", username, requestPath);
                            SecurityContextHolder.clearContext();
                            filterChain.doFilter(request, response);
                            return;
                        }
                        
                        // Log des autorités pour déboguer (INFO pour voir dans les logs)
                        logger.info("JWT Filter - User {} has authorities: {} on path: {}", 
                            username, userDetails.getAuthorities(), requestPath);
                        logger.info("JWT Filter - User enabled: {}", userDetails.isEnabled());
                        
                        // Créer l'authentification avec les autorités
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                userDetails.getAuthorities()
                            );
                        authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        
                        // Établir l'authentification dans le SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        logger.info("JWT Filter - Successfully authenticated user: {} with roles: {} on path: {}", 
                            username, userDetails.getAuthorities(), requestPath);
                        
                        // Vérifier que l'authentification est bien établie
                        Authentication verifiedAuth = SecurityContextHolder.getContext().getAuthentication();
                        if (verifiedAuth != null && verifiedAuth.isAuthenticated()) {
                            logger.info("JWT Filter - Authentication verified in SecurityContext: {} with authorities: {}", 
                                verifiedAuth.isAuthenticated(), verifiedAuth.getAuthorities());
                        } else {
                            logger.error("JWT Filter - WARNING: Authentication not properly set in SecurityContext!");
                        }
                    } catch (UsernameNotFoundException e) {
                        logger.error("User not found: {} on path: {}", username, requestPath, e);
                        SecurityContextHolder.clearContext();
                    } catch (Exception e) {
                        logger.error("Error loading user details for: {} on path: {}", username, requestPath, e);
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    logger.warn("Invalid JWT token on path: {}", requestPath);
                    if (requestPath.startsWith("/api/admin") || requestPath.startsWith("/api/auth")) {
                        logger.error("JWT Filter - Token validation failed for request to: {}", requestPath);
                    }
                    SecurityContextHolder.clearContext();
                }
            } else {
                if (requestPath.startsWith("/api/admin") || requestPath.startsWith("/api/auth")) {
                    logger.warn("JWT Filter - No JWT token found in request to: {}", requestPath);
                    logger.warn("JWT Filter - Authorization header value: {}", authHeader);
                    logger.warn("JWT Filter - All headers: {}", java.util.Collections.list(request.getHeaderNames()));
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context for path: {}", requestPath, ex);
            ex.printStackTrace();
            SecurityContextHolder.clearContext();
        }
        
        // Log final de l'état de l'authentification avant de passer au filtre suivant
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            logger.info("JWT Filter - After processing, authentication exists: {} with authorities: {} on path: {}", 
                auth.isAuthenticated(), auth.getAuthorities(), requestPath);
        } else {
            if (requestPath.startsWith("/api/admin") || requestPath.startsWith("/api/auth")) {
                logger.warn("JWT Filter - After processing, NO authentication on path: {}", requestPath);
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extrait le token JWT depuis le header Authorization
     * Format attendu: "Bearer <token>"
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}
