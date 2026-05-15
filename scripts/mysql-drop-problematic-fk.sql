-- Corrige l'échec Hibernate au démarrage, ex. :
-- SchemaManagementException: ... failed to find column mappings for foreign key named [FKgmmop32kneepq9dnh4ksd7eh6]
-- Cause fréquente : ancienne FK sur annonces.buyer_id → users (nom auto-généré Hibernate), métadonnées JDBC incohérentes.
--
-- Modèle Java : backend/src/main/java/com/vendit/model/Annonce.java — buyer_id, @ForeignKey(name = "fk_annonce_buyer").
-- Après suppression de la contrainte défectueuse, redémarrer Spring Boot ; ddl-auto=update recréera fk_annonce_buyer si besoin.
--
-- Exécuter dans MySQL. Adapter USE vendit si votre base a un autre nom.

USE vendit;

-- Étape 1 — Lister les FK sur annonces (noms exacts sur votre machine)
SELECT CONSTRAINT_NAME AS fk_name
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'annonces'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY';

-- Étape 2 — Copier le bloc ci-dessous, décommenter UNE ligne ALTER (nom = résultat de l'étape 1), exécuter.

-- SET FOREIGN_KEY_CHECKS = 0;
-- ALTER TABLE annonces DROP FOREIGN KEY FKgmmop32kneepq9dnh4ksd7eh6;
-- ALTER TABLE annonces DROP FOREIGN KEY fk_annonce_buyer;
-- SET FOREIGN_KEY_CHECKS = 1;

-- Étape 3 — Redémarrer l'application.
