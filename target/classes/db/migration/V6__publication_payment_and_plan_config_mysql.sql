-- Mode de paiement publication, top pub sur tarifs, plans vendeur configurables admin

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'annonces' AND COLUMN_NAME = 'publication_payment_method');
SET @ddl := IF(@exists = 0,
    'ALTER TABLE annonces ADD COLUMN publication_payment_method VARCHAR(20) NOT NULL DEFAULT ''CREDITS''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'publication_tarifs' AND COLUMN_NAME = 'top_publication');
SET @ddl := IF(@exists = 0,
    'ALTER TABLE publication_tarifs ADD COLUMN top_publication TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS seller_plan_configs (
    plan_code VARCHAR(20) NOT NULL PRIMARY KEY,
    label VARCHAR(80) NOT NULL,
    monthly_price_fcfa DECIMAL(14, 2) NOT NULL DEFAULT 0,
    commission_percent DECIMAL(5, 2) NOT NULL DEFAULT 15,
    max_active_publications INT NOT NULL DEFAULT 5,
    monthly_boosts_included INT NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    display_order INT NOT NULL DEFAULT 0,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO seller_plan_configs (plan_code, label, monthly_price_fcfa, commission_percent, max_active_publications, monthly_boosts_included, active, display_order)
VALUES
    ('FREE', 'Gratuit', 0, 15, 5, 0, 1, 0),
    ('PRO', 'Pro', 2900, 8, 50, 3, 1, 1),
    ('PREMIUM', 'Premium', 9900, 5, -1, 10, 1, 2);
