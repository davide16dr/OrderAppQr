-- Codice Destinatario SDI per fatturazione elettronica (opzionale)
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS sid VARCHAR(20);

-- Metodo di pagamento scelto in fase di registrazione
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20);
