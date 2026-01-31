package com.vestisen.config;

import com.vestisen.model.User;
import com.vestisen.service.ActionLogService;
import com.vestisen.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepte toutes les requêtes et enregistre les actions (POST, PUT, DELETE, PATCH) dans action_logs.
 * Les requêtes GET ne sont pas loguées.
 * Passe le rôle utilisateur, User-Agent et message d'erreur pour des logs explicites.
 */
@Component
public class ActionLoggingInterceptor implements HandlerInterceptor {

    @Autowired
    private ActionLogService actionLogService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return;
        }

        Long userId = null;
        String username = "anonymous";
        String userRole = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            username = auth.getName();
            userId = userRepository.findByEmail(username).map(User::getId).orElse(null);
            userRole = userRepository.findByEmail(username).map(u -> u.getRole().name()).orElse(null);
        }

        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        int status = response.getStatus();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String errorMessage = ex != null ? ex.getMessage() : null;

        actionLogService.logAction(userId, username, userRole, method, requestUri, queryString,
                status, clientIp, userAgent, errorMessage);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
