package com.vendit.repository;

import com.vendit.model.SubscriptionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionAuditLogRepository extends JpaRepository<SubscriptionAuditLog, Long> {
}
