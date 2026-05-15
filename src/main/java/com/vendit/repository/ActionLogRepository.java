package com.vendit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vendit.model.ActionLog;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {

    Page<ActionLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ActionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ActionLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM ActionLog a WHERE " +
            "(:search IS NULL OR :search = '' OR LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.actionLabel) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(a.resourceType) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.requestUri) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:username IS NULL OR :username = '' OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "(:userRole IS NULL OR :userRole = '' OR a.userRole = :userRole) AND " +
            "(:resourceType IS NULL OR :resourceType = '' OR a.resourceType = :resourceType) AND " +
            "(:actionLabel IS NULL OR :actionLabel = '' OR LOWER(a.actionLabel) LIKE LOWER(CONCAT('%', :actionLabel, '%'))) AND " +
            "(:dateFrom IS NULL OR a.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR a.createdAt <= :dateTo) AND " +
            "(:success IS NULL OR a.success = :success) AND " +
            "(:httpMethod IS NULL OR :httpMethod = '' OR a.httpMethod = :httpMethod)")
    Page<ActionLog> search(
            @Param("search") String search,
            @Param("username") String username,
            @Param("userRole") String userRole,
            @Param("resourceType") String resourceType,
            @Param("actionLabel") String actionLabel,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("success") Boolean success,
            @Param("httpMethod") String httpMethod,
            Pageable pageable);

}
