-- Gestion des abonnements vendeur (spec : seller_subscriptions, audit, webhooks, ledger)

CREATE TABLE IF NOT EXISTS seller_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_type VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date DATETIME NULL,
    renewal_date DATETIME NULL,
    stripe_subscription_id VARCHAR(128) NULL,
    stripe_customer_id VARCHAR(128) NULL,
    boosts_remaining INT NOT NULL DEFAULT 0,
    commission_rate DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    scheduled_downgrade VARCHAR(20) NULL,
    downgrade_locked TINYINT(1) NOT NULL DEFAULT 0,
    grace_until DATETIME NULL,
    billing_cycle VARCHAR(20) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_seller_subscriptions_user (user_id),
    CONSTRAINT fk_seller_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS subscription_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NULL,
    previous_plan VARCHAR(20) NULL,
    new_plan VARCHAR(20) NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NULL,
    detail VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_audit_subscription FOREIGN KEY (subscription_id) REFERENCES seller_subscriptions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS stripe_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stripe_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stripe_webhook_event (stripe_event_id)
);

CREATE TABLE IF NOT EXISTS subscription_pending_checkouts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    checkout_id CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    target_plan VARCHAR(20) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    amount_cents BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    stripe_payment_intent_id VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    UNIQUE KEY uk_sub_pending_checkout (checkout_id),
    UNIQUE KEY uk_sub_pending_idempotency (idempotency_key),
    CONSTRAINT fk_sub_pending_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS subscription_financial_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    transaction_type VARCHAR(40) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'XOF',
    stripe_invoice_id VARCHAR(128) NULL,
    stripe_payment_intent_id VARCHAR(128) NULL,
    idempotency_key VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_fin_subscription FOREIGN KEY (subscription_id) REFERENCES seller_subscriptions(id) ON DELETE CASCADE,
    UNIQUE KEY uk_sub_fin_idempotency (idempotency_key)
);

-- Migration des données existantes depuis users
INSERT INTO seller_subscriptions (
    user_id, plan_type, status, start_date, renewal_date,
    stripe_subscription_id, boosts_remaining, commission_rate,
    downgrade_locked, grace_until, billing_cycle, version
)
SELECT
    u.id,
    COALESCE(u.seller_plan, 'FREE'),
    CASE
        WHEN u.plan_grace_until IS NOT NULL AND u.plan_grace_until > NOW() THEN 'PAST_DUE'
        WHEN COALESCE(u.seller_plan, 'FREE') = 'FREE' THEN 'ACTIVE'
        WHEN u.plan_period_end IS NOT NULL AND u.plan_period_end < NOW()
             AND (u.plan_grace_until IS NULL OR u.plan_grace_until < NOW()) THEN 'CANCELLED'
        ELSE 'ACTIVE'
    END,
    u.plan_period_start,
    u.plan_period_end,
    u.stripe_subscription_id,
    COALESCE(u.boosts_remaining, 0),
    CASE COALESCE(u.seller_plan, 'FREE')
        WHEN 'PRO' THEN 8.00
        WHEN 'PREMIUM' THEN 5.00
        ELSE 15.00
    END,
    CASE
        WHEN COALESCE(u.seller_plan, 'FREE') <> 'FREE'
             AND u.plan_period_end IS NOT NULL AND u.plan_period_end > NOW() THEN 1
        ELSE 0
    END,
    u.plan_grace_until,
    u.plan_billing_cycle,
    0
FROM users u
WHERE u.user_role IN ('VENDEUR', 'ADMIN')
  AND NOT EXISTS (SELECT 1 FROM seller_subscriptions s WHERE s.user_id = u.id);

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'annonces' AND COLUMN_NAME = 'plan_paused');
SET @ddl := IF(@exists = 0, 'ALTER TABLE annonces ADD COLUMN plan_paused TINYINT(1) NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
