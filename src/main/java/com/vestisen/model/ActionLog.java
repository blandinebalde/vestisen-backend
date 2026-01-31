package com.vestisen.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Table des logs : enregistre toutes les actions des utilisateurs (POST, PUT, DELETE, PATCH).
 * Les requêtes GET ne sont pas loguées.
 * Champs enrichis pour faciliter l'audit et l'analyse (rôle, ressource, succès, etc.).
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

    /** Email ou identifiant de connexion (ex: email, téléphone) */
    @Column(name = "username", length = 255)
    private String username;

    /** Rôle de l'utilisateur au moment de l'action (ADMIN, VENDEUR, USER) — null si non authentifié */
    @Column(name = "user_role", length = 20)
    private String userRole;

    /** Méthode HTTP : POST, PUT, DELETE, PATCH */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /** URI demandée (ex: /api/annonces, /api/admin/users/5) */
    @Column(name = "request_uri", nullable = false, length = 1024)
    private String requestUri;

    /** Type de ressource ciblée dérivé de l'URI (ex: annonce, user, category, payment, credit) */
    @Column(name = "resource_type", length = 64)
    private String resourceType;

    /** Identifiant de la ressource si présent dans le path (ex: /api/annonces/123 → 123) */
    @Column(name = "resource_id")
    private Long resourceId;

    /** Libellé lisible de l'action (ex: "Création annonce", "Suppression utilisateur", "Approbation annonce") */
    @Column(name = "action_label", length = 255)
    private String actionLabel;

    /** Query string éventuelle */
    @Column(name = "query_string", length = 1024)
    private String queryString;

    /** Code de réponse HTTP (ex: 200, 201, 400, 403, 404) */
    @Column(name = "response_status")
    private Integer responseStatus;

    /** Indique si la requête a abouti (2xx) — utile pour filtres et rapports */
    @Column(name = "success")
    private Boolean success;

    /** Adresse IP du client (ou première IP dans X-Forwarded-For si proxy) */
    @Column(name = "client_ip", length = 64)
    private String clientIp;

    /** User-Agent du client (navigateur, application) */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /** Message d'erreur ou d'exception éventuel (court) */
    @Column(name = "error_message", length = 512)
    private String errorMessage;

    /** Horodatage de l'action */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
