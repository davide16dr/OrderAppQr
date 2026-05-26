-- =====================================================
-- OrderApp Complete Database Setup
-- =====================================================
-- This script creates the complete database schema
-- and supporting database objects only.
--
-- Compatible with: PostgreSQL 12+
-- Works with: Render, Railway, AWS RDS, Heroku, Azure, etc.
--
-- Usage:
--   psql -h <host> -U <user> -d <database> -f orderapp-complete-db-setup.sql
--
-- =====================================================

-- =====================================================
-- PART 1: RESET (optional - comment out if adding to existing DB)
-- =====================================================
-- DROP SCHEMA IF EXISTS public CASCADE;
-- CREATE SCHEMA public;

-- =====================================================
-- PART 2: EXTENSIONS
-- =====================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================
-- PART 3: DOMAINS (Custom Types)
-- =====================================================
CREATE DOMAIN dom_slug AS varchar(100)
    CHECK (value ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$');

CREATE DOMAIN dom_status_short AS varchar(30)
    CHECK (LENGTH(TRIM(value)) > 0);

CREATE DOMAIN dom_label AS varchar(150)
    CHECK (LENGTH(TRIM(value)) > 0);

CREATE DOMAIN dom_name AS varchar(200)
    CHECK (LENGTH(TRIM(value)) > 0);

CREATE DOMAIN dom_price AS numeric(10,2)
    CHECK (value >= 0);

CREATE DOMAIN dom_qty AS integer
    CHECK (value > 0);

CREATE DOMAIN dom_email AS varchar(255)
    CHECK (POSITION('@' IN value) > 1);

CREATE DOMAIN dom_token AS varchar(120)
    CHECK (LENGTH(TRIM(value)) >= 16);

-- =====================================================
-- PART 4: MAIN TABLES
-- =====================================================

-- Tenants (Multi-tenant organizations)
CREATE TABLE IF NOT EXISTS tenants (
    id                          BIGSERIAL PRIMARY KEY,
    slug                        dom_slug NOT NULL UNIQUE,
    name                        varchar(255) NOT NULL,
    legal_name                  varchar(255),
    business_type               varchar(30) NOT NULL,
    status                      dom_status_short NOT NULL DEFAULT 'PENDING',
    timezone                    varchar(100) NOT NULL DEFAULT 'Europe/Rome',
    currency_code               char(3) NOT NULL DEFAULT 'EUR',
    subdomain                   dom_slug NOT NULL UNIQUE,

    vat_number                  varchar(50),
    business_email              dom_email,
    business_phone              varchar(50),
    address_line_1              varchar(255),
    address_line_2              varchar(255),
    city                        varchar(100),
    province                    varchar(100),
    postal_code                 varchar(20),
    country                     varchar(100) NOT NULL DEFAULT 'Italy',

    registration_source         varchar(30) NOT NULL DEFAULT 'SELF_SIGNUP',
    activation_date             timestamptz,
    approved_at                 timestamptz,
    approved_by_staff_user_id   bigint,

    branding_json               jsonb NOT NULL DEFAULT '{}'::jsonb,
    opening_config_json         jsonb NOT NULL DEFAULT '{}'::jsonb,
    enabled                     boolean NOT NULL DEFAULT true,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_tenants_business_type
        CHECK (business_type IN ('LIDO', 'BAR', 'RESTAURANT', 'NIGHTCLUB', 'OTHER')),
    CONSTRAINT chk_tenants_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED')),
    CONSTRAINT chk_tenants_registration_source
        CHECK (registration_source IN ('SELF_SIGNUP', 'INTERNAL_CREATE')),
    CONSTRAINT chk_tenants_currency
        CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_tenants_branding_json_object
        CHECK (jsonb_typeof(branding_json) = 'object'),
    CONSTRAINT chk_tenants_opening_json_object
        CHECK (jsonb_typeof(opening_config_json) = 'object')
);

-- Areas (Zones within a tenant)
CREATE TABLE IF NOT EXISTS areas (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            dom_label NOT NULL,
    display_order   integer NOT NULL DEFAULT 0,
    status          dom_status_short NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_areas_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT uq_areas_tenant_name
        UNIQUE (tenant_id, name)
);

-- Locations (Tables, umbrellas, sunbeds, etc.)
CREATE TABLE IF NOT EXISTS locations (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    area_id         BIGINT REFERENCES areas(id) ON DELETE SET NULL,
    type            varchar(30) NOT NULL,
    label           dom_label NOT NULL,
    status          dom_status_short NOT NULL DEFAULT 'ACTIVE',
    capacity        integer,
    metadata_json   jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_locations_type
        CHECK (type IN ('TABLE', 'UMBRELLA', 'SUNBED', 'VIP', 'ROOM', 'LOUNGE', 'GENERIC')),
    CONSTRAINT chk_locations_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_locations_capacity
        CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT chk_locations_metadata_json_object
        CHECK (jsonb_typeof(metadata_json) = 'object'),
    CONSTRAINT uq_locations_tenant_label
        UNIQUE (tenant_id, label)
);

-- Location Tokens (QR codes)
CREATE TABLE IF NOT EXISTS location_tokens (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    location_id         BIGINT NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    token               dom_token NOT NULL UNIQUE,
    status              dom_status_short NOT NULL DEFAULT 'ACTIVE',
    is_primary          boolean NOT NULL DEFAULT true,
    rotatable           boolean NOT NULL DEFAULT true,
    expires_at          timestamptz,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_location_tokens_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT chk_location_tokens_expiration
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_location_tokens_active_primary
    ON location_tokens(location_id)
    WHERE is_primary = true AND status = 'ACTIVE';

-- Categories (Menu categories per tenant)
CREATE TABLE IF NOT EXISTS categories (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name            dom_label NOT NULL,
    description     varchar(300),
    display_order   integer NOT NULL DEFAULT 0,
    status          dom_status_short NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_categories_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT uq_categories_tenant_name
        UNIQUE (tenant_id, name)
);

-- Global Categories (Shared catalog)
CREATE TABLE IF NOT EXISTS global_categories (
    id              BIGSERIAL PRIMARY KEY,
    code            varchar(100) NOT NULL UNIQUE,
    name            dom_label NOT NULL,
    description     varchar(300),
    display_order   integer NOT NULL DEFAULT 0,
    status          dom_status_short NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_global_categories_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

-- Global Products (Shared catalog)
CREATE TABLE IF NOT EXISTS global_products (
    id                  BIGSERIAL PRIMARY KEY,
    code                varchar(100) NOT NULL UNIQUE,
    name                dom_name NOT NULL,
    description         varchar(500),
    default_image_url   text,
    default_department  varchar(30) NOT NULL DEFAULT 'BAR',
    default_vat_rate    numeric(5,2) NOT NULL DEFAULT 10.00,
    status              dom_status_short NOT NULL DEFAULT 'ACTIVE',
    metadata_json       jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_global_products_department
        CHECK (default_department IN ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),
    CONSTRAINT chk_global_products_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_global_products_vat_rate
        CHECK (default_vat_rate >= 0 AND default_vat_rate <= 100),
    CONSTRAINT chk_global_products_metadata_json_object
        CHECK (jsonb_typeof(metadata_json) = 'object')
);

-- Global Category Products mapping
CREATE TABLE IF NOT EXISTS global_category_products (
    global_category_id BIGINT NOT NULL REFERENCES global_categories(id) ON DELETE CASCADE,
    global_product_id  BIGINT NOT NULL REFERENCES global_products(id) ON DELETE CASCADE,
    display_order      integer NOT NULL DEFAULT 0,
    PRIMARY KEY (global_category_id, global_product_id)
);

-- Tenant Products (Real products sold by tenant)
CREATE TABLE IF NOT EXISTS tenant_products (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    global_product_id       BIGINT REFERENCES global_products(id) ON DELETE SET NULL,
    sku                     varchar(50),
    name                    varchar(200) NOT NULL,
    description             varchar(500),
    price                   dom_price NOT NULL,
    image_url               text,
    department              varchar(30) NOT NULL DEFAULT 'BAR',
    vat_rate                numeric(5,2) NOT NULL DEFAULT 10.00,
    status                  dom_status_short NOT NULL DEFAULT 'ACTIVE',
    available_for_order     boolean NOT NULL DEFAULT true,
    is_customized           boolean NOT NULL DEFAULT false,
    metadata_json           jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_tenant_products_department
        CHECK (department IN ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),
    CONSTRAINT chk_tenant_products_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_tenant_products_vat_rate
        CHECK (vat_rate >= 0 AND vat_rate <= 100),
    CONSTRAINT chk_tenant_products_metadata_json_object
        CHECK (jsonb_typeof(metadata_json) = 'object'),
    CONSTRAINT uq_tenant_products_tenant_sku
        UNIQUE (tenant_id, sku)
);

-- Category Tenant Products mapping
CREATE TABLE IF NOT EXISTS category_tenant_products (
    category_id        BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    tenant_product_id  BIGINT NOT NULL REFERENCES tenant_products(id) ON DELETE CASCADE,
    display_order      integer NOT NULL DEFAULT 0,
    PRIMARY KEY (category_id, tenant_product_id)
);

-- Modifier Groups (e.g., Size, Toppings)
CREATE TABLE IF NOT EXISTS modifier_groups (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                dom_label NOT NULL,
    min_selectable      integer NOT NULL DEFAULT 0,
    max_selectable      integer,
    required            boolean NOT NULL DEFAULT false,
    status              dom_status_short NOT NULL DEFAULT 'ACTIVE',
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_modifier_groups_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_modifier_groups_min
        CHECK (min_selectable >= 0),
    CONSTRAINT chk_modifier_groups_max
        CHECK (max_selectable IS NULL OR max_selectable >= min_selectable),
    CONSTRAINT uq_modifier_groups_tenant_name
        UNIQUE (tenant_id, name)
);

-- Modifier Options (e.g., Small, Medium, Large)
CREATE TABLE IF NOT EXISTS modifier_options (
    id                  BIGSERIAL PRIMARY KEY,
    modifier_group_id   BIGINT NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    tenant_id           BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                dom_label NOT NULL,
    price_delta         dom_price NOT NULL DEFAULT 0,
    status              dom_status_short NOT NULL DEFAULT 'ACTIVE',
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_modifier_options_status
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT uq_modifier_options_group_name
        UNIQUE (modifier_group_id, name)
);

-- Tenant Product Modifier Groups mapping
CREATE TABLE IF NOT EXISTS tenant_product_modifier_groups (
    tenant_product_id    BIGINT NOT NULL REFERENCES tenant_products(id) ON DELETE CASCADE,
    modifier_group_id    BIGINT NOT NULL REFERENCES modifier_groups(id) ON DELETE CASCADE,
    PRIMARY KEY (tenant_product_id, modifier_group_id)
);

-- Staff Users
CREATE TABLE IF NOT EXISTS staff_users (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT REFERENCES tenants(id) ON DELETE CASCADE,
    first_name          varchar(100) NOT NULL,
    last_name           varchar(100) NOT NULL,
    email               dom_email NOT NULL,
    password_hash       varchar(255) NOT NULL,
    phone               varchar(50),
    is_primary_contact  boolean NOT NULL DEFAULT false,
    invited_at          timestamptz,
    activated_at        timestamptz,
    last_login_at       timestamptz,
    status              dom_status_short NOT NULL DEFAULT 'ACTIVE',
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_staff_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_users_tenant_email_ci
    ON staff_users (tenant_id, LOWER(email));

-- Staff Roles
CREATE TABLE IF NOT EXISTS staff_roles (
    id          BIGSERIAL PRIMARY KEY,
    code        varchar(50) NOT NULL UNIQUE,
    description varchar(255)
);

-- Staff User Roles mapping
CREATE TABLE IF NOT EXISTS staff_user_roles (
    staff_user_id BIGINT NOT NULL REFERENCES staff_users(id) ON DELETE CASCADE,
    role_id       BIGINT NOT NULL REFERENCES staff_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (staff_user_id, role_id)
);

-- Subscription Plans
CREATE TABLE IF NOT EXISTS subscription_plans (
    code                    varchar(50) PRIMARY KEY,
    name                    varchar(100) NOT NULL,
    description             varchar(300),
    price_monthly           numeric(10,2),
    price_yearly            numeric(10,2),
    max_locations           integer,
    max_staff_users         integer,
    max_products            integer,
    qr_batch_enabled        boolean NOT NULL DEFAULT false,
    realtime_dashboard      boolean NOT NULL DEFAULT true,
    global_catalog_enabled  boolean NOT NULL DEFAULT true,
    is_active               boolean NOT NULL DEFAULT true,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_subscription_plans_prices
        CHECK (
            (price_monthly IS NULL OR price_monthly >= 0) AND
            (price_yearly IS NULL OR price_yearly >= 0)
        ),
    CONSTRAINT chk_subscription_plans_limits
        CHECK (
            (max_locations IS NULL OR max_locations > 0) AND
            (max_staff_users IS NULL OR max_staff_users > 0) AND
            (max_products IS NULL OR max_products > 0)
        )
);

-- Tenant Subscriptions
CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id                          BIGSERIAL PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_code                   varchar(50) NOT NULL REFERENCES subscription_plans(code),
    status                      varchar(30) NOT NULL DEFAULT 'PENDING',
    billing_cycle               varchar(20) NOT NULL DEFAULT 'MONTHLY',
    payment_provider            varchar(50),
    provider_customer_id        varchar(255),
    provider_subscription_id    varchar(255),
    payment_status              varchar(30) NOT NULL DEFAULT 'PENDING',
    current_period_start        timestamptz,
    current_period_end          timestamptz,
    trial_ends_at               timestamptz,
    activated_at                timestamptz,
    cancelled_at                timestamptz,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_tenant_subscriptions_status
        CHECK (status IN ('PENDING', 'TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_tenant_subscriptions_billing_cycle
        CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT chk_tenant_subscriptions_payment_status
        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'NONE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_subscriptions_current
    ON tenant_subscriptions(tenant_id)
    WHERE status IN ('PENDING', 'TRIAL', 'ACTIVE', 'PAST_DUE');

-- Orders
CREATE TABLE IF NOT EXISTS orders (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    location_id             BIGINT NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    location_label_snapshot varchar(150) NOT NULL,
    area_name_snapshot      varchar(150),
    source                  varchar(20) NOT NULL DEFAULT 'QR',
    status                  varchar(30) NOT NULL DEFAULT 'NEW',
    payment_status          varchar(30) NOT NULL DEFAULT 'NONE',
    customer_note           varchar(500),
    internal_note           varchar(500),
    subtotal_amount         dom_price NOT NULL DEFAULT 0,
    total_amount            dom_price NOT NULL DEFAULT 0,
    created_by_staff_id     BIGINT REFERENCES staff_users(id) ON DELETE SET NULL,
    accepted_by_staff_id    BIGINT REFERENCES staff_users(id) ON DELETE SET NULL,
    delivered_by_staff_id   BIGINT REFERENCES staff_users(id) ON DELETE SET NULL,
    accepted_at             timestamptz,
    ready_at                timestamptz,
    delivered_at            timestamptz,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_orders_source
        CHECK (source IN ('QR', 'STAFF')),
    CONSTRAINT chk_orders_status
        CHECK (status IN ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_orders_payment_status
        CHECK (payment_status IN ('NONE', 'PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_orders_total_ge_subtotal
        CHECK (total_amount >= subtotal_amount),
    CONSTRAINT chk_orders_timestamps
        CHECK (
            (accepted_at IS NULL OR accepted_at >= created_at) AND
            (ready_at IS NULL OR ready_at >= created_at) AND
            (delivered_at IS NULL OR delivered_at >= created_at)
        )
);

-- Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id                          BIGSERIAL PRIMARY KEY,
    order_id                    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    tenant_id                   BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    tenant_product_id           BIGINT NOT NULL REFERENCES tenant_products(id) ON DELETE RESTRICT,
    product_name_snapshot       varchar(200) NOT NULL,
    unit_price_snapshot         dom_price NOT NULL,
    quantity                    dom_qty NOT NULL,
    line_total                  dom_price NOT NULL,
    department_snapshot         varchar(30) NOT NULL,
    notes                       varchar(300),
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_order_items_department
        CHECK (department_snapshot IN ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),
    CONSTRAINT chk_order_items_line_total
        CHECK (line_total = ROUND((unit_price_snapshot * quantity)::numeric, 2))
);

-- Order Item Modifier Options
CREATE TABLE IF NOT EXISTS order_item_modifier_options (
    order_item_id                 BIGINT NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    modifier_option_id            BIGINT NOT NULL REFERENCES modifier_options(id) ON DELETE RESTRICT,
    modifier_group_name_snapshot  varchar(150) NOT NULL,
    option_name_snapshot          varchar(150) NOT NULL,
    price_delta_snapshot          dom_price NOT NULL DEFAULT 0,
    PRIMARY KEY (order_item_id, modifier_option_id)
);

-- Order Status History
CREATE TABLE IF NOT EXISTS order_status_history (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status      varchar(30),
    new_status      varchar(30) NOT NULL,
    changed_by      BIGINT REFERENCES staff_users(id) ON DELETE SET NULL,
    changed_at      timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_order_status_history_old
        CHECK (old_status IS NULL OR old_status IN ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_order_status_history_new
        CHECK (new_status IN ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED'))
);

-- Business Registration Requests
CREATE TABLE IF NOT EXISTS business_registration_requests (
    id                          BIGSERIAL PRIMARY KEY,
    requested_slug              dom_slug NOT NULL,
    tenant_name                 varchar(255) NOT NULL,
    legal_name                  varchar(255),
    business_type               varchar(30) NOT NULL,
    vat_number                  varchar(50),
    business_email              dom_email NOT NULL,
    business_phone              varchar(50),
    address_line_1              varchar(255),
    address_line_2              varchar(255),
    city                        varchar(100),
    province                    varchar(100),
    postal_code                 varchar(20),
    country                     varchar(100) NOT NULL DEFAULT 'Italy',

    contact_first_name          varchar(100) NOT NULL,
    contact_last_name           varchar(100) NOT NULL,
    contact_email               dom_email NOT NULL,
    contact_phone               varchar(50),
    password_hash               varchar(255) NOT NULL,

    requested_plan_code         varchar(50),
    status                      varchar(30) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at                timestamptz NOT NULL DEFAULT now(),
    reviewed_at                 timestamptz,
    reviewed_by_staff_user_id   BIGINT REFERENCES staff_users(id) ON DELETE SET NULL,
    rejection_reason            varchar(500),
    tenant_id                   BIGINT REFERENCES tenants(id) ON DELETE SET NULL,
    created_at                  timestamptz NOT NULL DEFAULT now(),
    updated_at                  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_business_registration_business_type
        CHECK (business_type IN ('LIDO', 'BAR', 'RESTAURANT', 'NIGHTCLUB', 'OTHER')),
    CONSTRAINT chk_business_registration_status
        CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'CONVERTED'))
);

-- Tenant Status History
CREATE TABLE IF NOT EXISTS tenant_status_history (
    id                          BIGSERIAL PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    old_status                  varchar(30),
    new_status                  varchar(30) NOT NULL,
    changed_by_staff_user_id    BIGINT REFERENCES staff_users(id) ON DELETE SET NULL,
    reason                      varchar(500),
    changed_at                  timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_tenant_status_history_old
        CHECK (old_status IS NULL OR old_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED')),
    CONSTRAINT chk_tenant_status_history_new
        CHECK (new_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED'))
);

-- =====================================================
-- PART 5: INDEXES
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenants_business_email ON tenants(LOWER(business_email));

CREATE INDEX IF NOT EXISTS idx_areas_tenant ON areas(tenant_id);
CREATE INDEX IF NOT EXISTS idx_locations_tenant ON locations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_locations_area ON locations(area_id);
CREATE INDEX IF NOT EXISTS idx_location_tokens_tenant ON location_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_location_tokens_location ON location_tokens(location_id);

CREATE INDEX IF NOT EXISTS idx_global_products_status ON global_products(status);
CREATE INDEX IF NOT EXISTS idx_tenant_products_tenant ON tenant_products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_products_global ON tenant_products(global_product_id);
CREATE INDEX IF NOT EXISTS idx_tenant_products_tenant_status ON tenant_products(tenant_id, status, available_for_order);
CREATE INDEX IF NOT EXISTS idx_category_tenant_products_product ON category_tenant_products(tenant_product_id);

CREATE INDEX IF NOT EXISTS idx_modifier_groups_tenant ON modifier_groups(tenant_id);
CREATE INDEX IF NOT EXISTS idx_modifier_options_tenant ON modifier_options(tenant_id);

CREATE INDEX IF NOT EXISTS idx_staff_users_tenant ON staff_users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_staff_users_primary_contact ON staff_users(tenant_id, is_primary_contact);

CREATE UNIQUE INDEX IF NOT EXISTS uq_business_registration_requested_slug
    ON business_registration_requests(LOWER(requested_slug))
    WHERE status IN ('SUBMITTED', 'APPROVED');

CREATE INDEX IF NOT EXISTS idx_business_registration_status
    ON business_registration_requests(status);

CREATE INDEX IF NOT EXISTS idx_business_registration_contact_email
    ON business_registration_requests(LOWER(contact_email));

CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant
    ON tenant_subscriptions(tenant_id);

CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_status
    ON tenant_subscriptions(status);

CREATE INDEX IF NOT EXISTS idx_tenant_status_history_tenant
    ON tenant_status_history(tenant_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_tenant_status_created ON orders(tenant_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_location ON orders(location_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_tenant_product ON order_items(tenant_product_id);

-- =====================================================
-- PART 6: FUNCTIONS
-- =====================================================

-- Set updated_at timestamp
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    new.updated_at := now();
    RETURN new;
END;
$$;

-- Generate location token
CREATE OR REPLACE FUNCTION fn_generate_location_token()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF new.token IS NULL OR LENGTH(TRIM(new.token)) = 0 THEN
        new.token := ENCODE(gen_random_bytes(16), 'hex');
    END IF;
    RETURN new;
END;
$$;

-- Check location area belongs to tenant
CREATE OR REPLACE FUNCTION fn_check_location_area_tenant()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_area_tenant BIGINT;
BEGIN
    IF new.area_id IS NOT NULL THEN
        SELECT tenant_id INTO v_area_tenant
        FROM areas
        WHERE id = new.area_id;

        IF v_area_tenant IS NULL THEN
            RAISE EXCEPTION 'Area % inesistente', new.area_id;
        END IF;

        IF v_area_tenant <> new.tenant_id THEN
            RAISE EXCEPTION 'La area % non appartiene al tenant %', new.area_id, new.tenant_id;
        END IF;
    END IF;

    RETURN new;
END;
$$;

-- Check location token belongs to tenant
CREATE OR REPLACE FUNCTION fn_check_location_token_tenant()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_location_tenant BIGINT;
BEGIN
    SELECT tenant_id INTO v_location_tenant
    FROM locations
    WHERE id = new.location_id;

    IF v_location_tenant IS NULL THEN
        RAISE EXCEPTION 'Location % inesistente', new.location_id;
    END IF;

    IF v_location_tenant <> new.tenant_id THEN
        RAISE EXCEPTION 'Location % non appartiene al tenant %', new.location_id, new.tenant_id;
    END IF;

    RETURN new;
END;
$$;

-- Check category and product belong to same tenant
CREATE OR REPLACE FUNCTION fn_check_category_tenant_product_tenant()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_cat_tenant BIGINT;
    v_prod_tenant BIGINT;
BEGIN
    SELECT tenant_id INTO v_cat_tenant FROM categories WHERE id = new.category_id;
    SELECT tenant_id INTO v_prod_tenant FROM tenant_products WHERE id = new.tenant_product_id;

    IF v_cat_tenant IS NULL OR v_prod_tenant IS NULL THEN
        RAISE EXCEPTION 'Categoria o tenant_product inesistente';
    END IF;

    IF v_cat_tenant <> v_prod_tenant THEN
        RAISE EXCEPTION 'Categoria e tenant_product appartengono a tenant diversi';
    END IF;

    RETURN new;
END;
$$;

-- Check modifier option belongs to tenant
CREATE OR REPLACE FUNCTION fn_check_modifier_option_tenant()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_group_tenant BIGINT;
BEGIN
    SELECT tenant_id INTO v_group_tenant
    FROM modifier_groups
    WHERE id = new.modifier_group_id;

    IF v_group_tenant IS NULL THEN
        RAISE EXCEPTION 'Modifier group % inesistente', new.modifier_group_id;
    END IF;

    IF v_group_tenant <> new.tenant_id THEN
        RAISE EXCEPTION 'Modifier option e modifier group appartengono a tenant diversi';
    END IF;

    RETURN new;
END;
$$;

-- Check tenant product modifier group belong to same tenant
CREATE OR REPLACE FUNCTION fn_check_tenant_product_modifier_group_tenant()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_prod_tenant BIGINT;
    v_group_tenant BIGINT;
BEGIN
    SELECT tenant_id INTO v_prod_tenant FROM tenant_products WHERE id = new.tenant_product_id;
    SELECT tenant_id INTO v_group_tenant FROM modifier_groups WHERE id = new.modifier_group_id;

    IF v_prod_tenant IS NULL OR v_group_tenant IS NULL THEN
        RAISE EXCEPTION 'Tenant product o gruppo modificatori inesistente';
    END IF;

    IF v_prod_tenant <> v_group_tenant THEN
        RAISE EXCEPTION 'Tenant product e gruppo modificatori appartengono a tenant diversi';
    END IF;

    RETURN new;
END;
$$;

-- Check order location and tenant, set snapshots
CREATE OR REPLACE FUNCTION fn_check_order_tenant_and_snapshots()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_location_tenant BIGINT;
    v_location_label VARCHAR(150);
    v_area_name VARCHAR(150);
BEGIN
    SELECT l.tenant_id, l.label, a.name
      INTO v_location_tenant, v_location_label, v_area_name
    FROM locations l
    LEFT JOIN areas a ON a.id = l.area_id
    WHERE l.id = new.location_id;

    IF v_location_tenant IS NULL THEN
        RAISE EXCEPTION 'Location % inesistente', new.location_id;
    END IF;

    IF v_location_tenant <> new.tenant_id THEN
        RAISE EXCEPTION 'La location % non appartiene al tenant %', new.location_id, new.tenant_id;
    END IF;

    IF TG_OP = 'INSERT' THEN
        new.location_label_snapshot := v_location_label;
        new.area_name_snapshot := v_area_name;
    END IF;

    RETURN new;
END;
$$;

-- Check order item tenant and set snapshots
CREATE OR REPLACE FUNCTION fn_check_order_item_tenant_and_snapshots()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_order_tenant BIGINT;
    v_product_tenant BIGINT;
    v_product_name VARCHAR(200);
    v_product_price NUMERIC(10,2);
    v_product_department VARCHAR(30);
BEGIN
    SELECT tenant_id INTO v_order_tenant
    FROM orders
    WHERE id = new.order_id;

    SELECT tenant_id, name, price, department
      INTO v_product_tenant, v_product_name, v_product_price, v_product_department
    FROM tenant_products
    WHERE id = new.tenant_product_id;

    IF v_order_tenant IS NULL THEN
        RAISE EXCEPTION 'Ordine % inesistente', new.order_id;
    END IF;

    IF v_product_tenant IS NULL THEN
        RAISE EXCEPTION 'Prodotto tenant % inesistente', new.tenant_product_id;
    END IF;

    IF new.tenant_id <> v_order_tenant THEN
        RAISE EXCEPTION 'Order item con tenant incoerente rispetto all''ordine';
    END IF;

    IF new.tenant_id <> v_product_tenant THEN
        RAISE EXCEPTION 'Order item con tenant incoerente rispetto al prodotto tenant';
    END IF;

    IF TG_OP = 'INSERT' THEN
        new.product_name_snapshot := v_product_name;
        new.unit_price_snapshot := v_product_price;
        new.department_snapshot := v_product_department;
    END IF;

    new.line_total := ROUND((new.unit_price_snapshot * new.quantity)::numeric, 2);

    RETURN new;
END;
$$;

-- Check order item modifier option
CREATE OR REPLACE FUNCTION fn_check_order_item_modifier_option()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_order_tenant BIGINT;
    v_option_tenant BIGINT;
    v_group_name VARCHAR(150);
    v_option_name VARCHAR(150);
    v_price_delta NUMERIC(10,2);
BEGIN
    SELECT o.tenant_id
      INTO v_order_tenant
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.id
    WHERE oi.id = new.order_item_id;

    SELECT mo.tenant_id, mg.name, mo.name, mo.price_delta
      INTO v_option_tenant, v_group_name, v_option_name, v_price_delta
    FROM modifier_options mo
    JOIN modifier_groups mg ON mg.id = mo.modifier_group_id
    WHERE mo.id = new.modifier_option_id;

    IF v_option_tenant IS NULL THEN
        RAISE EXCEPTION 'Modifier option % inesistente', new.modifier_option_id;
    END IF;

    IF v_order_tenant <> v_option_tenant THEN
        RAISE EXCEPTION 'Modifier option e order item appartengono a tenant diversi';
    END IF;

    IF TG_OP = 'INSERT' THEN
        new.modifier_group_name_snapshot := v_group_name;
        new.option_name_snapshot := v_option_name;
        new.price_delta_snapshot := v_price_delta;
    END IF;

    RETURN new;
END;
$$;

-- Fill tenant product from global product
CREATE OR REPLACE FUNCTION fn_fill_tenant_product_from_global()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_name VARCHAR(200);
    v_description VARCHAR(500);
    v_image TEXT;
    v_department VARCHAR(30);
    v_vat NUMERIC(5,2);
BEGIN
    IF new.global_product_id IS NOT NULL THEN
        SELECT name, description, default_image_url, default_department, default_vat_rate
          INTO v_name, v_description, v_image, v_department, v_vat
        FROM global_products
        WHERE id = new.global_product_id AND status = 'ACTIVE';

        IF v_name IS NULL THEN
            RAISE EXCEPTION 'Global product % inesistente o non attivo', new.global_product_id;
        END IF;

        new.name := COALESCE(new.name, v_name);
        new.description := COALESCE(new.description, v_description);
        new.image_url := COALESCE(new.image_url, v_image);
        new.department := COALESCE(new.department, v_department);
        new.vat_rate := COALESCE(new.vat_rate, v_vat);
    END IF;

    RETURN new;
END;
$$;

-- Recalculate order totals
CREATE OR REPLACE FUNCTION fn_recalculate_order_totals(p_order_id BIGINT)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_items_total NUMERIC(10,2);
    v_modifiers_total NUMERIC(10,2);
BEGIN
    SELECT COALESCE(SUM(line_total), 0)
      INTO v_items_total
    FROM order_items
    WHERE order_id = p_order_id;

    SELECT COALESCE(SUM(oi.quantity * oimo.price_delta_snapshot), 0)
      INTO v_modifiers_total
    FROM order_items oi
    JOIN order_item_modifier_options oimo ON oimo.order_item_id = oi.id
    WHERE oi.order_id = p_order_id;

    UPDATE orders
       SET subtotal_amount = ROUND(v_items_total + v_modifiers_total, 2),
           total_amount    = ROUND(v_items_total + v_modifiers_total, 2),
           updated_at      = now()
     WHERE id = p_order_id;
END;
$$;

-- Order totals trigger
CREATE OR REPLACE FUNCTION fn_order_totals_trigger()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_order_id BIGINT;
BEGIN
    v_order_id := COALESCE(new.order_id, old.order_id);
    PERFORM fn_recalculate_order_totals(v_order_id);
    RETURN NULL;
END;
$$;

-- Prevent edit closed orders
CREATE OR REPLACE FUNCTION fn_prevent_edit_closed_orders()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_status VARCHAR(30);
    v_order_id BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'order_item_modifier_options' THEN
        SELECT oi.order_id
          INTO v_order_id
        FROM order_items oi
        WHERE oi.id = COALESCE(new.order_item_id, old.order_item_id);
    ELSE
        v_order_id := COALESCE(new.order_id, old.order_id);
    END IF;

    SELECT status INTO v_status
    FROM orders
    WHERE id = v_order_id;

    IF v_status IN ('DELIVERED', 'CANCELLED') THEN
        RAISE EXCEPTION 'Impossibile modificare righe di un ordine con stato %', v_status;
    END IF;

    RETURN COALESCE(new, old);
END;
$$;

-- Validate order status transition
CREATE OR REPLACE FUNCTION fn_validate_order_status_transition()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND old.status <> new.status THEN
        IF old.status = 'NEW' AND new.status NOT IN ('ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED') THEN
            RAISE EXCEPTION 'Transizione non valida da NEW a %', new.status;
        ELSIF old.status IN ('DELIVERED', 'CANCELLED') AND new.status <> old.status THEN
            RAISE EXCEPTION 'Ordine finale: impossibile cambiare stato da % a %', old.status, new.status;
        END IF;

        IF new.status = 'DELIVERED' AND new.delivered_at IS NULL THEN
            new.delivered_at := now();
        END IF;
    END IF;

    RETURN new;
END;
$$;

-- Insert order status history
CREATE OR REPLACE FUNCTION fn_insert_order_status_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO order_status_history(order_id, old_status, new_status, changed_by, changed_at)
        VALUES (new.id, NULL, new.status, new.created_by_staff_id, now());
    ELSIF TG_OP = 'UPDATE' AND old.status <> new.status THEN
        INSERT INTO order_status_history(order_id, old_status, new_status, changed_by, changed_at)
        VALUES (
            new.id,
            old.status,
            new.status,
            COALESCE(new.delivered_by_staff_id, new.created_by_staff_id),
            now()
        );
    END IF;

    RETURN new;
END;
$$;

-- Insert tenant status history
CREATE OR REPLACE FUNCTION fn_insert_tenant_status_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO tenant_status_history(
            tenant_id,
            old_status,
            new_status,
            changed_by_staff_user_id,
            reason,
            changed_at
        )
        VALUES (
            new.id,
            NULL,
            new.status,
            new.approved_by_staff_user_id,
            'Tenant created',
            now()
        );

    ELSIF TG_OP = 'UPDATE' AND old.status <> new.status THEN
        INSERT INTO tenant_status_history(
            tenant_id,
            old_status,
            new_status,
            changed_by_staff_user_id,
            reason,
            changed_at
        )
        VALUES (
            new.id,
            old.status,
            new.status,
            new.approved_by_staff_user_id,
            'Tenant status changed',
            now()
        );
    END IF;

    RETURN new;
END;
$$;

-- Validate tenant activation
CREATE OR REPLACE FUNCTION fn_validate_tenant_activation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND old.status <> new.status THEN
        IF new.status = 'ACTIVE' THEN
            IF new.activation_date IS NULL THEN
                new.activation_date := now();
            END IF;

            IF new.approved_at IS NULL THEN
                new.approved_at := now();
            END IF;
        END IF;

        IF old.status = 'DISABLED' AND new.status <> 'DISABLED' THEN
            RAISE EXCEPTION 'Tenant DISABLED non può essere riattivato automaticamente';
        END IF;
    END IF;

    RETURN new;
END;
$$;

-- =====================================================
-- PART 7: TRIGGERS
-- =====================================================
CREATE TRIGGER IF NOT EXISTS trg_tenants_updated_at
BEFORE UPDATE ON tenants
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_areas_updated_at
BEFORE UPDATE ON areas
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_locations_updated_at
BEFORE UPDATE ON locations
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_location_tokens_updated_at
BEFORE UPDATE ON location_tokens
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_categories_updated_at
BEFORE UPDATE ON categories
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_global_categories_updated_at
BEFORE UPDATE ON global_categories
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_global_products_updated_at
BEFORE UPDATE ON global_products
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_tenant_products_updated_at
BEFORE UPDATE ON tenant_products
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_modifier_groups_updated_at
BEFORE UPDATE ON modifier_groups
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_modifier_options_updated_at
BEFORE UPDATE ON modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_staff_users_updated_at
BEFORE UPDATE ON staff_users
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_business_registration_requests_updated_at
BEFORE UPDATE ON business_registration_requests
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_tenant_subscriptions_updated_at
BEFORE UPDATE ON tenant_subscriptions
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_order_items_updated_at
BEFORE UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER IF NOT EXISTS trg_location_tokens_generate
BEFORE INSERT ON location_tokens
FOR EACH ROW EXECUTE FUNCTION fn_generate_location_token();

CREATE TRIGGER IF NOT EXISTS trg_locations_area_tenant
BEFORE INSERT OR UPDATE ON locations
FOR EACH ROW EXECUTE FUNCTION fn_check_location_area_tenant();

CREATE TRIGGER IF NOT EXISTS trg_location_tokens_tenant
BEFORE INSERT OR UPDATE ON location_tokens
FOR EACH ROW EXECUTE FUNCTION fn_check_location_token_tenant();

CREATE TRIGGER IF NOT EXISTS trg_category_tenant_products_tenant
BEFORE INSERT OR UPDATE ON category_tenant_products
FOR EACH ROW EXECUTE FUNCTION fn_check_category_tenant_product_tenant();

CREATE TRIGGER IF NOT EXISTS trg_modifier_options_tenant
BEFORE INSERT OR UPDATE ON modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_check_modifier_option_tenant();

CREATE TRIGGER IF NOT EXISTS trg_tenant_product_modifier_groups_tenant
BEFORE INSERT OR UPDATE ON tenant_product_modifier_groups
FOR EACH ROW EXECUTE FUNCTION fn_check_tenant_product_modifier_group_tenant();

CREATE TRIGGER IF NOT EXISTS trg_orders_tenant
BEFORE INSERT OR UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION fn_check_order_tenant_and_snapshots();

CREATE TRIGGER IF NOT EXISTS trg_order_items_tenant
BEFORE INSERT OR UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_check_order_item_tenant_and_snapshots();

CREATE TRIGGER IF NOT EXISTS trg_order_item_modifier_options_tenant
BEFORE INSERT OR UPDATE ON order_item_modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_check_order_item_modifier_option();

CREATE TRIGGER IF NOT EXISTS trg_tenant_products_fill_from_global
BEFORE INSERT ON tenant_products
FOR EACH ROW EXECUTE FUNCTION fn_fill_tenant_product_from_global();

CREATE TRIGGER IF NOT EXISTS trg_order_items_prevent_closed_edit_ins
BEFORE INSERT ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_prevent_edit_closed_orders();

CREATE TRIGGER IF NOT EXISTS trg_order_items_prevent_closed_edit_upd
BEFORE UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_prevent_edit_closed_orders();

CREATE TRIGGER IF NOT EXISTS trg_order_items_prevent_closed_edit_del
BEFORE DELETE ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_prevent_edit_closed_orders();

CREATE TRIGGER IF NOT EXISTS trg_order_item_mod_prevent_closed_edit_ins
BEFORE INSERT ON order_item_modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_prevent_edit_closed_orders();

CREATE TRIGGER IF NOT EXISTS trg_order_item_mod_prevent_closed_edit_upd
BEFORE UPDATE ON order_item_modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_prevent_edit_closed_orders();

CREATE TRIGGER IF NOT EXISTS trg_order_item_mod_prevent_closed_edit_del
BEFORE DELETE ON order_item_modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_prevent_edit_closed_orders();

CREATE TRIGGER IF NOT EXISTS trg_tenants_validate_activation
BEFORE UPDATE ON tenants
FOR EACH ROW EXECUTE FUNCTION fn_validate_tenant_activation();

CREATE TRIGGER IF NOT EXISTS trg_tenants_status_history
AFTER INSERT OR UPDATE ON tenants
FOR EACH ROW EXECUTE FUNCTION fn_insert_tenant_status_history();

CREATE TRIGGER IF NOT EXISTS trg_orders_validate_status_transition
BEFORE UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION fn_validate_order_status_transition();

CREATE TRIGGER IF NOT EXISTS trg_orders_status_history
AFTER INSERT OR UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION fn_insert_order_status_history();

CREATE TRIGGER IF NOT EXISTS trg_order_items_recalc_ins
AFTER INSERT ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_order_totals_trigger();

CREATE TRIGGER IF NOT EXISTS trg_order_items_recalc_upd
AFTER UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_order_totals_trigger();

CREATE TRIGGER IF NOT EXISTS trg_order_items_recalc_del
AFTER DELETE ON order_items
FOR EACH ROW EXECUTE FUNCTION fn_order_totals_trigger();

CREATE TRIGGER IF NOT EXISTS trg_order_item_mod_recalc_ins
AFTER INSERT ON order_item_modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_order_totals_trigger();

CREATE TRIGGER IF NOT EXISTS trg_order_item_mod_recalc_upd
AFTER UPDATE ON order_item_modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_order_totals_trigger();

CREATE TRIGGER IF NOT EXISTS trg_order_item_mod_recalc_del
AFTER DELETE ON order_item_modifier_options
FOR EACH ROW EXECUTE FUNCTION fn_order_totals_trigger();

-- =====================================================
-- PART 8: DEMO DATA
-- =====================================================

-- Minimal demo seed: one super admin and one tenant.
INSERT INTO staff_roles(code, description) VALUES
('MANAGER', 'Super admin del sistema');

INSERT INTO staff_users(tenant_id, first_name, last_name, email, password_hash, status, is_primary_contact, activated_at, last_login_at, created_at, updated_at) VALUES
(NULL, 'Admin', 'System', 'admin@orderapp.local', '$2a$10$SlVZzG4zJQEVE8Ev3ZzV.OjF2RQ4g8NeC5p5Dg9nV5nFfL5.qiKG6', 'ACTIVE', false, now(), now(), now(), now());

INSERT INTO staff_user_roles(staff_user_id, role_id) VALUES
((SELECT id FROM staff_users WHERE email='admin@orderapp.local'), (SELECT id FROM staff_roles WHERE code='MANAGER'));

INSERT INTO tenants(
    slug,
    name,
    legal_name,
    business_type,
    status,
    timezone,
    currency_code,
    subdomain,
    registration_source,
    enabled,
    approved_at,
    activation_date,
    created_at,
    updated_at
) VALUES (
    'demo-orderapp',
    'Demo OrderApp',
    'Demo OrderApp S.r.l.',
    'RESTAURANT',
    'ACTIVE',
    'Europe/Rome',
    'EUR',
    'demo',
    'INTERNAL_CREATE',
    true,
    now(),
    now(),
    now(),
    now()
);

-- =====================================================
-- END OF SCRIPT
-- =====================================================
