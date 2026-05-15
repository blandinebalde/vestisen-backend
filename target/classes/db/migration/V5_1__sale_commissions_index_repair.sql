-- Réparation si V5 a échoué sur CREATE INDEX IF NOT EXISTS (table déjà créée sans index)
-- Ignorer l'erreur "Duplicate key name" si l'index existe déjà.

CREATE INDEX idx_sale_commissions_seller ON sale_commissions(seller_id, created_at);
