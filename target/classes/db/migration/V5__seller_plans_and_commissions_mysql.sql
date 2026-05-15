-- Plans vendeur (Gratuit / Pro / Premium) et commissions sur ventes

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS seller_plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ADD COLUMN IF NOT EXISTS plan_billing_cycle VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS plan_period_start DATETIME NULL,
    ADD COLUMN IF NOT EXISTS plan_period_end DATETIME NULL,
    ADD COLUMN IF NOT EXISTS plan_grace_until DATETIME NULL,
    ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(128) NULL,
    ADD COLUMN IF NOT EXISTS boosts_remaining INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS boosts_period_start DATETIME NULL;

CREATE TABLE IF NOT EXISTS sale_commissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    annonce_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    sale_amount_fcfa DECIMAL(14, 2) NOT NULL,
    commission_percent DECIMAL(5, 2) NOT NULL,
    commission_amount_fcfa DECIMAL(14, 2) NOT NULL,
    seller_net_fcfa DECIMAL(14, 2) NOT NULL,
    seller_plan_at_sale VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sale_commissions_public_id UNIQUE (public_id),
    CONSTRAINT uk_sale_commissions_annonce UNIQUE (annonce_id),
    CONSTRAINT fk_sale_commissions_annonce FOREIGN KEY (annonce_id) REFERENCES annonces(id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_commissions_seller FOREIGN KEY (seller_id) REFERENCES users(id),
    CONSTRAINT fk_sale_commissions_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
    KEY idx_sale_commissions_seller (seller_id, created_at)
);
