package com.vestisen.repository;

import com.vestisen.model.ActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {

    List<ActionLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<ActionLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ActionLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
