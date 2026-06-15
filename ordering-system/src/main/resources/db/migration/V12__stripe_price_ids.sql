-- Add Stripe price IDs to subscription_plans for Checkout Session creation
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS stripe_price_id_monthly VARCHAR(100),
    ADD COLUMN IF NOT EXISTS stripe_price_id_yearly  VARCHAR(100);
