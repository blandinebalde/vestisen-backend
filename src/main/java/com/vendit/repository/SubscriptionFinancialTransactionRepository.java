package com.vendit.repository;

import com.vendit.model.SubscriptionFinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionFinancialTransactionRepository extends JpaRepository<SubscriptionFinancialTransaction, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
