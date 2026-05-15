-- Réparation Flyway après un échec sur la migration V3 (annonces_rename_condition_column).
-- À exécuter dans MySQL sur la base concernée (ex. vendit), puis redémarrer Spring Boot.
--
-- Supprime uniquement les lignes d'échec pour la version 3 ; la migration sera rejouée au prochain démarrage.

USE vendit;

DELETE FROM flyway_schema_history
WHERE version = '3'
  AND success = 0;
