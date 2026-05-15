-- MySQL uniquement : exécuté par Flyway avant Hibernate (spring.jpa.hibernate.ddl-auto=update).
-- Sans table physique, Hibernate peut tenter ALTER ... ADD FK sur annonces inexistante.
-- Les FK sont laissées à Hibernate sur le démarrage suivant.

CREATE TABLE IF NOT EXISTS annonces (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id CHAR(36) DEFAULT NULL,
  code VARCHAR(18) DEFAULT NULL,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(2000) DEFAULT NULL,
  price DECIMAL(19,2) NOT NULL,
  category_id BIGINT NOT NULL,
  publication_type VARCHAR(100) NOT NULL DEFAULT 'Standard',
  publication_credit_cost DECIMAL(12,2) DEFAULT NULL,
  `condition` VARCHAR(50) DEFAULT NULL,
  size VARCHAR(255) DEFAULT NULL,
  brand VARCHAR(255) DEFAULT NULL,
  color VARCHAR(255) DEFAULT NULL,
  location VARCHAR(255) DEFAULT NULL,
  seller_id BIGINT NOT NULL,
  buyer_id BIGINT DEFAULT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  view_count INT NOT NULL DEFAULT 0,
  contact_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) DEFAULT NULL,
  published_at DATETIME(6) DEFAULT NULL,
  expires_at DATETIME(6) DEFAULT NULL,
  tout_doit_partir TINYINT(1) NOT NULL DEFAULT 0,
  original_price DECIMAL(19,2) DEFAULT NULL,
  is_lot TINYINT(1) NOT NULL DEFAULT 0,
  accept_payment_on_delivery TINYINT(1) NOT NULL DEFAULT 0,
  latitude DOUBLE DEFAULT NULL,
  longitude DOUBLE DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_annonces_public_id (public_id),
  UNIQUE KEY UK_annonces_code (code),
  KEY idx_annonces_category (category_id),
  KEY idx_annonces_seller (seller_id),
  KEY idx_annonces_buyer (buyer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS annonce_images (
  annonce_id BIGINT NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  KEY idx_annonce_images_annonce (annonce_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
