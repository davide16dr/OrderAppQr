-- Aggiorna i price ID Stripe al piano LIVE
-- Sostituisci i valori placeholder con i price_id reali presi dalla Dashboard Stripe (modalità LIVE)
UPDATE subscription_plans
SET stripe_price_id_monthly = 'price_1TyW4pGGdVojdl5PJ86FsPN3',
    stripe_price_id_yearly  = 'price_1TyW4pGGdVojdl5PpcsDaFBw',
    updated_at = now()
WHERE code = 'BASIC';
