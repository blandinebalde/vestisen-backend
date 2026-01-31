package com.vestisen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Table des logs : enregistre toutes les actions des utilisateurs (POST, PUT, DELETE, PATCH).
 * Les requêtes GET ne sont pas loguées.
 */
@Entity
@Table(name = "action_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'utilisateur (null si non authentifié) */
    @Column(name = "user_id")
    private Long userId;

    /** Email ou nom d'utilisateur */
    @Column(name = "username", length = 255)
    private String username;

    /** Méthode HTTP : POST, PUT, DELETE, PATCH */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /** URI demandée (ex: /api/annonces) */
    @Column(name = "request_uri", nullable = false, length = 1024)
    private String requestUri;

    /** Query string éventuelle */
    @Column(name = "query_string", length = 1024)
    private String queryString;

    /** Code de réponse HTTP (ex: 200, 404) */
    @Column(name = "response_status")
    private Integer responseStatus;

    /** Adresse IP du client */
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    /** Horodatage de l'action */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
