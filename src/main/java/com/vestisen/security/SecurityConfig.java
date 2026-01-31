package com.vestisen.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * Configuration de sécurité Spring Security
 * Gère l'authentification JWT, les autorisations par rôle, et CORS
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    /**
     * Configuration du PasswordEncoder (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength 12 pour plus de sécurité
    }
    
    /**
     * Configuration du DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false); // Pour mieux gérer les erreurs
        return authProvider;
    }
    
    /**
     * Configuration de l'AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * Configuration principale de la chaîne de sécurité
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configuration CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Désactivation CSRF (non nécessaire avec JWT stateless)
            .csrf(csrf -> csrf.disable())
            
            // Configuration des sessions (stateless pour JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configuration des autorisations
            // IMPORTANT: Tous les endpoints protégés utilisent .authenticated()
            // La vérification des rôles (ADMIN, VENDEUR, USER) se fait au niveau des contrôleurs
            // avec @PreAuthorize ou directement dans le code
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics (pas d'authentification requise)
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/verify-email",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/annonces/public/**",
                    "/api/tarifs/**",
                    "/api/categories",
                    "/api/config/**",
                    "/api/credits/config",
                    "/error",
                    "/actuator/health"
                ).permitAll()
                // Photos des annonces (stockées dans uploads/images/annonce/user/...)
                .requestMatchers("/annonce/**").permitAll()
            
                
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            
            // Gestion des exceptions d'accès refusé
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    
                    if (auth != null) {
                        logger.error("Access denied for user: {} with authorities: {} on path: {}", 
                            auth.getName(), auth.getAuthorities(), request.getRequestURI());
                        logger.error("User authenticated: {}", auth.isAuthenticated());
                    } else {
                        logger.error("Access denied for unauthenticated user on path: {}", request.getRequestURI());
                    }
                    
                    logger.error("AccessDeniedException message: {}", accessDeniedException.getMessage());
                    logger.error("Request method: {}, URI: {}", request.getMethod(), request.getRequestURI());
                    
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Access Denied\",\"message\":\"" + 
                        accessDeniedException.getMessage() + 
                        "\",\"path\":\"" + request.getRequestURI() + "\"}"
                    );
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.error("Unauthorized access attempt on path: {}", request.getRequestURI());
                    logger.error("Authentication exception: {}", authException.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"" + 
                        authException.getMessage() + 
                        "\",\"path\":\"" + request.getRequestURI() + "\"}"
                    );
                })
            )
            
            // Ajout du filtre JWT avant le filtre d'authentification par défaut
            // IMPORTANT: Le filtre JWT doit être ajouté avant UsernamePasswordAuthenticationFilter
            // pour qu'il puisse établir l'authentification dans le SecurityContext
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configuration des headers de sécurité
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            );
        
        return http.build();
    }
    
    /**
     * Configuration CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origines autorisées
        configuration.setAllowedOrigins(Arrays.asList(
            frontendUrl,
            "http://localhost:4200",
            "http://127.0.0.1:4200"
        ));
        
        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Headers exposés au client
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // Autoriser les credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);
        
        // Durée de mise en cache de la pré-requête CORS
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
