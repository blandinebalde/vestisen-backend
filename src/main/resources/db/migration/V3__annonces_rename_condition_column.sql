-- Idempotent (MySQL) : renomme `condition` -> article_condition seulement si la colonne `condition` existe.
-- Évite l'échec Flyway quand la table a déjà été créée avec `article_condition` (ex. script manuel).
--
-- Si le démarrage affiche encore : "failed migration to version 3" :
--   exécuter une fois scripts/mysql-flyway-clear-failed-migration-v3.sql sur la base vendit, puis redémarrer.

SELECT COUNT(*) INTO @has_condition
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'annonces'
  AND COLUMN_NAME = 'condition';

SET @sql = IF(@has_condition > 0,
  'ALTER TABLE annonces CHANGE COLUMN `condition` article_condition VARCHAR(50) NULL',
  'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
