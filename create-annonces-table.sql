-- Script SQL pour créer manuellement la table annonces si elle n'existe pas
-- Exécutez ce script dans MySQL si Hibernate ne crée pas la table

USE vestisen;

-- Créer la table annonces si elle n'existe pas
CREATE TABLE IF NOT EXISTS annonces (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(19, 2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    publication_type VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    condition VARCHAR(50),
    size VARCHAR(50),
    brand VARCHAR(255),
    color VARCHAR(255),
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    view_count INT DEFAULT 0,
    contact_count INT DEFAULT 0,
    seller_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    published_at DATETIME(6),
    expires_at DATETIME(6),
    FOREIGN KEY (seller_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
