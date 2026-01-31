package com.vestisen.service;

import com.vestisen.model.ActionLog;
import com.vestisen.repository.ActionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service de logging des actions utilisateur (hors GET).
 * Persiste chaque action POST, PUT, DELETE, PATCH dans la table action_logs
 * avec champs enrichis (rôle, type de ressource, libellé, succès, user-agent).
 */
@Service
public class ActionLogService {

    private static final Logger logger = LoggerFactory.getLogger(ActionLogService.class);
    private static final Pattern PATH_ID_PATTERN = Pattern.compile("/api(?:/admin)?/[^/]+/(\\d+)");

    @Autowired
    private ActionLogRepository actionLogRepository;

    /**
     * Enregistre une action métier interne (sans requête HTTP) : approbation, passage en Standard, etc.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logInternalAction(Long userId, String username, String userRole, String actionLabel,
                                  String resourceType, Long resourceId, boolean success) {
        try {
            ActionLog log = new ActionLog();
            log.setUserId(userId);
            log.setUsername(username != null ? username : "system");
            log.setUserRole(userRole);
            log.setHttpMethod("INTERNAL");
            log.setRequestUri("internal");
            log.setResourceType(resourceType);
            log.setResourceId(resourceId);
            log.setActionLabel(actionLabel);
            log.setQueryString(null);
            log.setResponseStatus(200);
            log.setSuccess(success);
            log.setClientIp(null);
            log.setUserAgent(null);
            log.setErrorMessage(null);
            log.setCreatedAt(LocalDateTime.now());
            actionLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to persist internal action log: {}", e.getMessage());
        }
    }

    /**
     * Enregistre une action (appelé par l'interceptor pour chaque requête non-GET).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logAction(Long userId, String username, String userRole, String httpMethod, String requestUri,
                          String queryString, Integer responseStatus, String clientIp, String userAgent,
                          String errorMessage) {
        try {
            ActionLog log = new ActionLog();
            log.setUserId(userId);
            log.setUsername(username != null ? username : "anonymous");
            log.setUserRole(userRole);
            log.setHttpMethod(httpMethod);
            log.setRequestUri(requestUri);
            log.setResourceType(deriveResourceType(requestUri));
            log.setResourceId(deriveResourceId(requestUri));
            log.setActionLabel(buildActionLabel(httpMethod, requestUri));
            log.setQueryString(queryString);
            log.setResponseStatus(responseStatus);
            log.setSuccess(responseStatus != null && responseStatus >= 200 && responseStatus < 300);
            log.setClientIp(clientIp);
            log.setUserAgent(userAgent != null && userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent);
            log.setErrorMessage(errorMessage != null && errorMessage.length() > 512 ? errorMessage.substring(0, 512) : errorMessage);
            log.setCreatedAt(LocalDateTime.now());
            actionLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to persist action log: {}", e.getMessage());
        }
    }

    private String deriveResourceType(String requestUri) {
        if (requestUri == null || !requestUri.startsWith("/api")) return null;
        String path = requestUri.contains("?") ? requestUri.substring(0, requestUri.indexOf('?')) : requestUri;
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if ("api".equals(segments[i]) && i + 1 < segments.length) {
                String next = segments[i + 1];
                if ("admin".equals(next) && i + 2 < segments.length) return segments[i + 2];
                return next;
            }
        }
        return null;
    }

    private Long deriveResourceId(String requestUri) {
        if (requestUri == null) return null;
        String path = requestUri.contains("?") ? requestUri.substring(0, requestUri.indexOf('?')) : requestUri;
        Matcher m = PATH_ID_PATTERN.matcher(path);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private String buildActionLabel(String httpMethod, String requestUri) {
        String resource = deriveResourceType(requestUri);
        if (resource == null) resource = "ressource";
        String resourceFr = mapResourceToLabel(resource);
        return switch (httpMethod != null ? httpMethod.toUpperCase() : "") {
            case "POST" -> isApproveRejectPath(requestUri) ? labelApproveReject(requestUri, resourceFr)
                    : (isCreatePath(requestUri) ? "Création " + resourceFr : "Action POST " + resourceFr);
            case "PUT", "PATCH" -> "Modification " + resourceFr;
            case "DELETE" -> "Suppression " + resourceFr;
            default -> httpMethod + " " + resourceFr;
        };
    }

    private String mapResourceToLabel(String segment) {
        return switch (segment.toLowerCase()) {
            case "annonces" -> "annonce";
            case "users" -> "utilisateur";
            case "categories" -> "catégorie";
            case "tarifs" -> "tarif";
            case "credits" -> "crédit";
            case "payments" -> "paiement";
            case "reviews" -> "avis";
            case "conversations", "messages" -> "conversation";
            case "cart" -> "panier";
            default -> segment;
        };
    }

    private boolean isApproveRejectPath(String uri) {
        return uri != null && (uri.contains("/approve") || uri.contains("/reject"));
    }

    private boolean isCreatePath(String uri) {
        if (uri == null) return false;
        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
        return path.matches("/api(?:/admin)?/[^/]+/?$") || path.matches("/api(?:/admin)?/[^/]+/\\d+/[^/]+");
    }

    private String labelApproveReject(String requestUri, String resourceFr) {
        return requestUri != null && requestUri.contains("/reject") ? "Rejet " + resourceFr : "Approbation " + resourceFr;
    }

    public Page<ActionLog> findAll(Pageable pageable) {
        return actionLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<ActionLog> findByUserId(Long userId, Pageable pageable) {
        return actionLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<ActionLog> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return actionLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }
}
