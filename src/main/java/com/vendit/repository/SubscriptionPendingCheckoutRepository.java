package com.vendit.repository;

import com.vendit.model.SubscriptionPendingCheckout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPendingCheckoutRepository extends JpaRepository<SubscriptionPendingCheckout, Long> {
    Optional<SubscriptionPendingCheckout> findByCheckoutId(String checkoutId);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
