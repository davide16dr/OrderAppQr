-- Migration per aggiungere colonne di supporto all'autenticazione

-- Verifica se la colonna password_hash esiste già, altrimenti la aggiunge
-- (Potrebbe già esistere dal database precedente)
ALTER TABLE staff_users
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NOT NULL DEFAULT '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36P4/KFm';

-- Aggiorna la colonna status se non ha un default
ALTER TABLE staff_users
ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Crea un indice su email per velocizzare le query di login
CREATE INDEX IF NOT EXISTS idx_staff_users_email ON staff_users(LOWER(email));

-- Crea un indice su tenant_id e email per query veloci del findByTenantIdAndEmail
CREATE INDEX IF NOT EXISTS idx_staff_users_tenant_email ON staff_users(tenant_id, LOWER(email));

-- Aggiorna la tabella staff_users per assicurarsi che abbia le colonne richieste
ALTER TABLE staff_users
ADD COLUMN IF NOT EXISTS is_primary_contact BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE staff_users
ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE;

-- Crea la tabella refresh_tokens per tracciare i refresh token (opzionale ma consigliato)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    staff_user_id BIGINT NOT NULL REFERENCES staff_users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Crea un indice su staff_user_id e expires_at per query efficienti
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(staff_user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Commento sulla password di default (è una password BCrypt dummy, deve essere cambiata)
COMMENT ON COLUMN staff_users.password_hash IS 'Password hash BCrypt. Default è una password dummy che deve essere cambiata dai veri utenti durante l''onboarding.';
