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

/**
 * Service de logging des actions utilisateur (hors GET).
 * Persiste chaque action POST, PUT, DELETE, PATCH dans la table action_logs.
 */
@Service
public class ActionLogService {

    private static final Logger logger = LoggerFactory.getLogger(ActionLogService.class);

    @Autowired
    private ActionLogRepository actionLogRepository;

    /**
     * Enregistre une action (appelé par l'interceptor pour chaque requête non-GET).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void logAction(Long userId, String username, String httpMethod, String requestUri,
                          String queryString, Integer responseStatus, String clientIp) {
        try {
            ActionLog log = new ActionLog();
            log.setUserId(userId);
            log.setUsername(username != null ? username : "anonymous");
            log.setHttpMethod(httpMethod);
            log.setRequestUri(requestUri);
            log.setQueryString(queryString);
            log.setResponseStatus(responseStatus);
            log.setClientIp(clientIp);
            log.setCreatedAt(LocalDateTime.now());
            actionLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to persist action log: {}", e.getMessage());
        }
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
