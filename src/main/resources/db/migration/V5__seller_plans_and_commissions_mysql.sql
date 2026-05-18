-- Plans vendeur (Gratuit / Pro / Premium) et commissions sur ventes
-- SQL idempotent (MySQL / MariaDB sans ADD COLUMN IF NOT EXISTS)

-- Colonnes plan vendeur sur users
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'seller_plan');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN seller_plan VARCHAR(20) NOT NULL DEFAULT ''FREE''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'plan_billing_cycle');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN plan_billing_cycle VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'plan_period_start');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN plan_period_start DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'plan_period_end');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN plan_period_end DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'plan_grace_until');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN plan_grace_until DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'stripe_subscription_id');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN stripe_subscription_id VARCHAR(128) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'boosts_remaining');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN boosts_remaining INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'boosts_period_start');
SET @ddl := IF(@exists = 0, 'ALTER TABLE users ADD COLUMN boosts_period_start DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

-- Index de secours si la table existait déjà sans KEY (échec migration précédente)
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sale_commissions' AND INDEX_NAME = 'idx_sale_commissions_seller');
SET @ddl := IF(@idx = 0,
    'ALTER TABLE sale_commissions ADD INDEX idx_sale_commissions_seller (seller_id, created_at)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
