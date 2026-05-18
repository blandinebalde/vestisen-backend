package com.vendit.repository;

import com.vendit.model.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, Long> {
    boolean existsByStripeEventId(String stripeEventId);
}
