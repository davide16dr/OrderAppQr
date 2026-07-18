-- Unico piano attivo: €59/mese, €590/anno
UPDATE subscription_plans
SET name          = 'Piano Pro',
    description   = 'Tutto il necessario per il tuo locale',
    price_monthly = 59.00,
    price_yearly  = 590.00,
    updated_at    = now()
WHERE code = 'BASIC';

-- Rimuovi i piani non più in uso
DELETE FROM subscription_plans WHERE code IN ('PROFESSIONAL', 'ENTERPRISE');
