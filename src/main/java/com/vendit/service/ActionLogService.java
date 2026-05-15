
package com.vendit.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.vendit.dto.ActionLogDTO;
import com.vendit.dto.ActionLogFilterRequest;
import com.vendit.model.ActionLog;
import com.vendit.repository.ActionLogRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public Page<ActionLogDTO> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return actionLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDTO);
    }

    public List<ActionLog> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return actionLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    public Page<ActionLogDTO> search(ActionLogFilterRequest filter) {
        String search = (filter.getSearch() != null && filter.getSearch().trim().isEmpty()) ? null : filter.getSearch();
        String username = (filter.getUsername() != null && filter.getUsername().trim().isEmpty()) ? null : filter.getUsername();
        String userRole = (filter.getUserRole() != null && filter.getUserRole().trim().isEmpty()) ? null : filter.getUserRole();
        String resourceType = (filter.getResourceType() != null && filter.getResourceType().trim().isEmpty()) ? null : filter.getResourceType();
        String actionLabel = (filter.getActionLabel() != null && filter.getActionLabel().trim().isEmpty()) ? null : filter.getActionLabel();
        String httpMethod = (filter.getHttpMethod() != null && filter.getHttpMethod().trim().isEmpty()) ? null : filter.getHttpMethod();
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActionLog> page = actionLogRepository.search(
                search, username, userRole, resourceType, actionLabel,
                filter.getDateFromAsDateTime(), filter.getDateToAsDateTime(),
                filter.getSuccess(), httpMethod, pageable);
        return page.map(this::toDTO);
    }

    public byte[] exportToExcel(ActionLogFilterRequest filter) {
        String search = (filter.getSearch() != null && filter.getSearch().trim().isEmpty()) ? null : filter.getSearch();
        String username = (filter.getUsername() != null && filter.getUsername().trim().isEmpty()) ? null : filter.getUsername();
        String userRole = (filter.getUserRole() != null && filter.getUserRole().trim().isEmpty()) ? null : filter.getUserRole();
        String resourceType = (filter.getResourceType() != null && filter.getResourceType().trim().isEmpty()) ? null : filter.getResourceType();
        String actionLabel = (filter.getActionLabel() != null && filter.getActionLabel().trim().isEmpty()) ? null : filter.getActionLabel();
        String httpMethod = (filter.getHttpMethod() != null && filter.getHttpMethod().trim().isEmpty()) ? null : filter.getHttpMethod();
        Pageable limit = PageRequest.of(0, 50_000, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActionLog> page = actionLogRepository.search(
                search, username, userRole, resourceType, actionLabel,
                filter.getDateFromAsDateTime(), filter.getDateToAsDateTime(),
                filter.getSuccess(), httpMethod, limit);
        List<ActionLog> list = page.getContent();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Logs");
            Row header = sheet.createRow(0);
            String[] headers = { "Id", "Date", "Utilisateur", "Rôle", "Méthode", "URI", "Type ressource", "Id ressource", "Action", "Statut HTTP", "Succès", "IP", "Erreur" };
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            int rowNum = 1;
            for (ActionLog log : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId() != null ? log.getId() : 0);
                row.createCell(1).setCellValue(log.getCreatedAt() != null ? log.getCreatedAt().format(formatter) : "");
                row.createCell(2).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(3).setCellValue(log.getUserRole() != null ? log.getUserRole() : "");
                row.createCell(4).setCellValue(log.getHttpMethod() != null ? log.getHttpMethod() : "");
                row.createCell(5).setCellValue(log.getRequestUri() != null ? log.getRequestUri() : "");
                row.createCell(6).setCellValue(log.getResourceType() != null ? log.getResourceType() : "");
                row.createCell(7).setCellValue(log.getResourceId() != null ? log.getResourceId() : 0);
                row.createCell(8).setCellValue(log.getActionLabel() != null ? log.getActionLabel() : "");
                row.createCell(9).setCellValue(log.getResponseStatus() != null ? log.getResponseStatus() : 0);
                row.createCell(10).setCellValue(log.getSuccess() != null ? log.getSuccess() ? "Oui" : "Non" : "");
                row.createCell(11).setCellValue(log.getClientIp() != null ? log.getClientIp() : "");
                row.createCell(12).setCellValue(log.getErrorMessage() != null ? log.getErrorMessage() : "");
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            logger.error("Export Excel logs failed", e);
            throw new RuntimeException("Export Excel échoué: " + e.getMessage());
        }
    }

    private ActionLogDTO toDTO(ActionLog log) {
        ActionLogDTO dto = new ActionLogDTO();
        dto.setId(log.getId());
        dto.setUserId(log.getUserId());
        dto.setUsername(log.getUsername());
        dto.setUserRole(log.getUserRole());
        dto.setHttpMethod(log.getHttpMethod());
        dto.setRequestUri(log.getRequestUri());
        dto.setResourceType(log.getResourceType());
        dto.setResourceId(log.getResourceId());
        dto.setActionLabel(log.getActionLabel());
        dto.setQueryString(log.getQueryString());
        dto.setResponseStatus(log.getResponseStatus());
        dto.setSuccess(log.getSuccess());
        dto.setClientIp(log.getClientIp());
        dto.setUserAgent(log.getUserAgent());
        dto.setErrorMessage(log.getErrorMessage());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
