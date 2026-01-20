-- Script pour vérifier et ajouter les colonnes de vérification si elles n'existent pas
USE vestisen;

-- Vérifier si les colonnes existent
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'vestisen' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME IN ('verification_token', 'verification_token_expiry', 'reset_password_token', 'reset_password_expiry', 'email_verified');

-- Ajouter les colonnes si elles n'existent pas
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS verification_token_expiry DATETIME,
ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255),
ADD COLUMN IF NOT EXISTS reset_password_expiry DATETIME,
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(255);

-- Vérifier les utilisateurs avec leurs tokens
SELECT id, email, email_verified, verification_token, verification_token_expiry, enabled
FROM users
WHERE verification_token IS NOT NULL;
