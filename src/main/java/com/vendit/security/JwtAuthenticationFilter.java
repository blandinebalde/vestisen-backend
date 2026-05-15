package com.vendit.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                java.util.Optional<ParsedAccessToken> parsedOpt = tokenProvider.parseAndValidateAccessToken(jwt);
                if (parsedOpt.isEmpty()) {
                    SecurityContextHolder.clearContext();
                } else {
                    ParsedAccessToken parsed = parsedOpt.get();
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(parsed.subject());

                        if (!userDetails.isEnabled()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Disabled user rejected: {} on {}", parsed.subject(), requestPath);
                            }
                            SecurityContextHolder.clearContext();
                        } else if (!(userDetails instanceof AppUserDetails aud)) {
                            logger.warn("Unexpected UserDetails type for {}; rejecting JWT", parsed.subject());
                            SecurityContextHolder.clearContext();
                        } else if (aud.getTokenVersion() != parsed.tokenVersion()) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("JWT token version mismatch for {}", parsed.subject());
                            }
                            SecurityContextHolder.clearContext();
                        } else {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    } catch (UsernameNotFoundException e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("User not found for JWT: {} on {}", parsed.subject(), requestPath);
                        }
                        SecurityContextHolder.clearContext();
                    } catch (Exception e) {
                        logger.error("JWT user load failed for path {}", requestPath, e);
                        SecurityContextHolder.clearContext();
                    }
                }
            } else {
                SecurityContextHolder.clearContext();
            }
        } catch (Exception ex) {
            logger.error("JWT filter error for path {}", requestPath, ex);
            SecurityContextHolder.clearContext();
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
