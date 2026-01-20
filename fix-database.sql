-- Script SQL pour corriger la base de données
-- Exécutez ce script dans MySQL pour supprimer toutes les tables et laisser Hibernate les recréer

USE vestisen;

-- Désactiver les vérifications de clés étrangères
SET FOREIGN_KEY_CHECKS = 0;

-- Supprimer toutes les tables dans l'ordre inverse des dépendances
DROP TABLE IF EXISTS annonce_images;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS annonces;
DROP TABLE IF EXISTS publication_tarifs;
DROP TABLE IF EXISTS users;

-- Réactiver les vérifications de clés étrangères
SET FOREIGN_KEY_CHECKS = 1;

-- Vérifier que toutes les tables ont été supprimées
SHOW TABLES;
