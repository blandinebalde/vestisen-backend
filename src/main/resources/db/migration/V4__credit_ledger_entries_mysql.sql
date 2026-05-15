-- Grand livre des mouvements de crédits (MySQL).
-- Pas de FK vers credit_transactions / annonces : Flyway s'exécute avant Hibernate sur une base neuve.
CREATE TABLE IF NOT EXISTS credit_ledger_entries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) DEFAULT NULL,
  user_id BIGINT NOT NULL,
  movement_type VARCHAR(40) NOT NULL,
  amount_delta DECIMAL(12,2) NOT NULL,
  balance_after DECIMAL(12,2) NOT NULL,
  credit_transaction_id BIGINT DEFAULT NULL,
  annonce_id BIGINT DEFAULT NULL,
  reference_code VARCHAR(64) DEFAULT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_credit_ledger_public_id (public_id),
  KEY idx_credit_ledger_user_created (user_id, created_at),
  KEY idx_credit_ledger_credit_tx (credit_transaction_id),
  KEY idx_credit_ledger_annonce (annonce_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
