-- =========================================================
-- 1) ESTENSIONI
-- =========================================================
create extension if not exists pgcrypto;

-- =========================================================
-- 2) DOMAINS
-- =========================================================
create domain dom_slug as varchar(100)
    check (value ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$');

create domain dom_status_short as varchar(30)
    check (length(trim(value)) > 0);

create domain dom_label as varchar(150)
    check (length(trim(value)) > 0);

create domain dom_name as varchar(200)
    check (length(trim(value)) > 0);

create domain dom_price as numeric(10,2)
    check (value >= 0);

create domain dom_qty as integer
    check (value > 0);

create domain dom_email as varchar(255)
    check (position('@' in value) > 1);

create domain dom_token as varchar(120)
    check (length(trim(value)) >= 16);

-- =========================================================
-- 3) TENANTS
-- =========================================================
create table tenants (
    id                          bigserial primary key,
    slug                        dom_slug not null unique,
    name                        varchar(255) not null,
    legal_name                  varchar(255),
    business_type               varchar(30) not null,
    status                      dom_status_short not null default 'PENDING',
    timezone                    varchar(100) not null default 'Europe/Rome',
    currency_code               char(3) not null default 'EUR',
    subdomain                   dom_slug not null unique,

    vat_number                  varchar(50),
    business_email              dom_email,
    business_phone              varchar(50),
    address_line_1              varchar(255),
    address_line_2              varchar(255),
    city                        varchar(100),
    province                    varchar(100),
    postal_code                 varchar(20),
    country                     varchar(100) not null default 'Italy',

    registration_source         varchar(30) not null default 'SELF_SIGNUP',
    activation_date             timestamptz,
    approved_at                 timestamptz,
    approved_by_staff_user_id   bigint null,

    branding_json               jsonb not null default '{}'::jsonb,
    opening_config_json         jsonb not null default '{}'::jsonb,
    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz not null default now(),

    constraint chk_tenants_business_type
        check (business_type in ('LIDO', 'BAR', 'RESTAURANT', 'NIGHTCLUB', 'OTHER')),

    constraint chk_tenants_status
        check (status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED')),

    constraint chk_tenants_registration_source
        check (registration_source in ('SELF_SIGNUP', 'INTERNAL_CREATE')),

    constraint chk_tenants_currency
        check (currency_code ~ '^[A-Z]{3}$'),

    constraint chk_tenants_branding_json_object
        check (jsonb_typeof(branding_json) = 'object'),

    constraint chk_tenants_opening_json_object
        check (jsonb_typeof(opening_config_json) = 'object')
);

-- =========================================================
-- 4) AREE E LOCAZIONI
-- =========================================================
create table areas (
    id              bigserial primary key,
    tenant_id       bigint not null references tenants(id) on delete cascade,
    name            dom_label not null,
    display_order   integer not null default 0,
    status          dom_status_short not null default 'ACTIVE',
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),

    constraint chk_areas_status
        check (status in ('ACTIVE', 'DISABLED')),

    constraint uq_areas_tenant_name
        unique (tenant_id, name)
);

create table locations (
    id              bigserial primary key,
    tenant_id       bigint not null references tenants(id) on delete cascade,
    area_id         bigint null references areas(id) on delete set null,
    type            varchar(30) not null,
    label           dom_label not null,
    status          dom_status_short not null default 'ACTIVE',
    capacity        integer null,
    metadata_json   jsonb not null default '{}'::jsonb,
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),

    constraint chk_locations_type
        check (type in ('TABLE', 'UMBRELLA', 'SUNBED', 'VIP', 'ROOM', 'LOUNGE', 'GENERIC')),

    constraint chk_locations_status
        check (status in ('ACTIVE', 'DISABLED')),

    constraint chk_locations_capacity
        check (capacity is null or capacity > 0),

    constraint chk_locations_metadata_json_object
        check (jsonb_typeof(metadata_json) = 'object'),

    constraint uq_locations_tenant_label
        unique (tenant_id, label)
);

-- =========================================================
-- 5) TOKEN QR DELLE LOCAZIONI
-- =========================================================
create table location_tokens (
    id                  bigserial primary key,
    tenant_id           bigint not null references tenants(id) on delete cascade,
    location_id         bigint not null references locations(id) on delete cascade,
    token               dom_token not null unique,
    status              dom_status_short not null default 'ACTIVE',
    is_primary          boolean not null default true,
    rotatable           boolean not null default true,
    expires_at          timestamptz null,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),

    constraint chk_location_tokens_status
        check (status in ('ACTIVE', 'REVOKED', 'EXPIRED')),

    constraint chk_location_tokens_expiration
        check (expires_at is null or expires_at > created_at)
);

create unique index uq_location_tokens_active_primary
    on location_tokens(location_id)
    where is_primary = true and status = 'ACTIVE';

-- =========================================================
-- 6) CATEGORIE TENANT
-- =========================================================
create table categories (
    id              bigserial primary key,
    tenant_id       bigint not null references tenants(id) on delete cascade,
    name            dom_label not null,
    description     varchar(300),
    display_order   integer not null default 0,
    status          dom_status_short not null default 'ACTIVE',
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),

    constraint chk_categories_status
        check (status in ('ACTIVE', 'DISABLED')),

    constraint uq_categories_tenant_name
        unique (tenant_id, name)
);

-- =========================================================
-- 7) CATALOGO GLOBALE
-- =========================================================
create table global_categories (
    id              bigserial primary key,
    code            varchar(100) not null unique,
    name            dom_label not null,
    description     varchar(300),
    display_order   integer not null default 0,
    status          dom_status_short not null default 'ACTIVE',
    created_at      timestamptz not null default now(),
    updated_at      timestamptz not null default now(),

    constraint chk_global_categories_status
        check (status in ('ACTIVE', 'DISABLED'))
);

create table global_products (
    id                  bigserial primary key,
    code                varchar(100) not null unique,
    name                dom_name not null,
    description         varchar(500),
    default_image_url   text,
    default_department  varchar(30) not null default 'BAR',
    default_vat_rate    numeric(5,2) not null default 10.00,
    status              dom_status_short not null default 'ACTIVE',
    metadata_json       jsonb not null default '{}'::jsonb,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),

    constraint chk_global_products_department
        check (default_department in ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),

    constraint chk_global_products_status
        check (status in ('ACTIVE', 'DISABLED')),

    constraint chk_global_products_vat_rate
        check (default_vat_rate >= 0 and default_vat_rate <= 100),

    constraint chk_global_products_metadata_json_object
        check (jsonb_typeof(metadata_json) = 'object')
);

create table global_category_products (
    global_category_id bigint not null references global_categories(id) on delete cascade,
    global_product_id  bigint not null references global_products(id) on delete cascade,
    display_order      integer not null default 0,
    primary key (global_category_id, global_product_id)
);

-- =========================================================
-- 8) PRODOTTI TENANT (REALI PRODOTTI VENDUTI)
-- =========================================================
create table tenant_products (
    id                      bigserial primary key,
    tenant_id               bigint not null references tenants(id) on delete cascade,
    global_product_id       bigint null references global_products(id) on delete set null,
    sku                     varchar(50),
    name                    varchar(200) not null,
    description             varchar(500),
    price                   dom_price not null,
    image_url               text,
    department              varchar(30) not null default 'BAR',
    vat_rate                numeric(5,2) not null default 10.00,
    status                  dom_status_short not null default 'ACTIVE',
    available_for_order     boolean not null default true,
    is_customized           boolean not null default false,
    metadata_json           jsonb not null default '{}'::jsonb,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now(),

    constraint chk_tenant_products_department
        check (department in ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),

    constraint chk_tenant_products_status
        check (status in ('ACTIVE', 'DISABLED')),

    constraint chk_tenant_products_vat_rate
        check (vat_rate >= 0 and vat_rate <= 100),

    constraint chk_tenant_products_metadata_json_object
        check (jsonb_typeof(metadata_json) = 'object'),

    constraint uq_tenant_products_tenant_sku
        unique (tenant_id, sku)
);

create table category_tenant_products (
    category_id        bigint not null references categories(id) on delete cascade,
    tenant_product_id  bigint not null references tenant_products(id) on delete cascade,
    display_order      integer not null default 0,
    primary key (category_id, tenant_product_id)
);

-- =========================================================
-- 9) MODIFICATORI
-- =========================================================
create table modifier_groups (
    id                  bigserial primary key,
    tenant_id           bigint not null references tenants(id) on delete cascade,
    name                dom_label not null,
    min_selectable      integer not null default 0,
    max_selectable      integer null,
    required            boolean not null default false,
    status              dom_status_short not null default 'ACTIVE',
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),

    constraint chk_modifier_groups_status
        check (status in ('ACTIVE', 'DISABLED')),

    constraint chk_modifier_groups_min
        check (min_selectable >= 0),

    constraint chk_modifier_groups_max
        check (max_selectable is null or max_selectable >= min_selectable),

    constraint uq_modifier_groups_tenant_name
        unique (tenant_id, name)
);

create table modifier_options (
    id                  bigserial primary key,
    modifier_group_id   bigint not null references modifier_groups(id) on delete cascade,
    tenant_id           bigint not null references tenants(id) on delete cascade,
    name                dom_label not null,
    price_delta         dom_price not null default 0,
    status              dom_status_short not null default 'ACTIVE',
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),

    constraint chk_modifier_options_status
        check (status in ('ACTIVE', 'DISABLED')),

    constraint uq_modifier_options_group_name
        unique (modifier_group_id, name)
);

create table tenant_product_modifier_groups (
    tenant_product_id    bigint not null references tenant_products(id) on delete cascade,
    modifier_group_id    bigint not null references modifier_groups(id) on delete cascade,
    primary key (tenant_product_id, modifier_group_id)
);

-- =========================================================
-- 10) STAFF
-- =========================================================
create table staff_users (
    id                  bigserial primary key,
    tenant_id           bigint not null references tenants(id) on delete cascade,
    first_name          varchar(100) not null,
    last_name           varchar(100) not null,
    email               dom_email not null,
    password_hash       varchar(255) not null,
    phone               varchar(50),
    is_primary_contact  boolean not null default false,
    invited_at          timestamptz,
    activated_at        timestamptz,
    last_login_at       timestamptz,
    status              dom_status_short not null default 'ACTIVE',
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now(),

    constraint chk_staff_users_status
        check (status in ('ACTIVE', 'DISABLED'))
);

create unique index uq_staff_users_tenant_email_ci
    on staff_users (tenant_id, lower(email));

create table staff_roles (
    id          bigserial primary key,
    code        varchar(50) not null unique,
    description varchar(255)
);

create table staff_user_roles (
    staff_user_id bigint not null references staff_users(id) on delete cascade,
    role_id       bigint not null references staff_roles(id) on delete cascade,
    primary key (staff_user_id, role_id)
);

alter table tenants
    add constraint fk_tenants_approved_by_staff
    foreign key (approved_by_staff_user_id)
    references staff_users(id)
    on delete set null;

-- =========================================================
-- 11) REGISTRAZIONE AUTONOMA AZIENDA
-- =========================================================
create table business_registration_requests (
    id                          bigserial primary key,
    requested_slug              dom_slug not null,
    tenant_name                 varchar(255) not null,
    legal_name                  varchar(255),
    business_type               varchar(30) not null,
    vat_number                  varchar(50),
    business_email              dom_email not null,
    business_phone              varchar(50),
    address_line_1              varchar(255),
    address_line_2              varchar(255),
    city                        varchar(100),
    province                    varchar(100),
    postal_code                 varchar(20),
    country                     varchar(100) not null default 'Italy',

    contact_first_name          varchar(100) not null,
    contact_last_name           varchar(100) not null,
    contact_email               dom_email not null,
    contact_phone               varchar(50),
    password_hash               varchar(255) not null,

    requested_plan_code         varchar(50),
    status                      varchar(30) not null default 'SUBMITTED',
    submitted_at                timestamptz not null default now(),
    reviewed_at                 timestamptz,
    reviewed_by_staff_user_id   bigint null references staff_users(id) on delete set null,
    rejection_reason            varchar(500),
    tenant_id                   bigint null references tenants(id) on delete set null,
    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz not null default now(),

    constraint chk_business_registration_business_type
        check (business_type in ('LIDO', 'BAR', 'RESTAURANT', 'NIGHTCLUB', 'OTHER')),

    constraint chk_business_registration_status
        check (status in ('SUBMITTED', 'APPROVED', 'REJECTED', 'CONVERTED'))
);

-- =========================================================
-- ) SUBSCRIPTIONS PLANS
-- =========================================================

create table subscription_plans (
    code                    varchar(50) primary key,
    name                    varchar(100) not null,
    description             varchar(300),
    price_monthly           numeric(10,2),
    price_yearly            numeric(10,2),
    max_locations           integer,
    max_staff_users         integer,
    max_products            integer,
    qr_batch_enabled        boolean not null default false,
    realtime_dashboard      boolean not null default true,
    global_catalog_enabled  boolean not null default true,
    is_active               boolean not null default true,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now(),

    constraint chk_subscription_plans_prices
        check (
            (price_monthly is null or price_monthly >= 0) and
            (price_yearly is null or price_yearly >= 0)
        ),

    constraint chk_subscription_plans_limits
        check (
            (max_locations is null or max_locations > 0) and
            (max_staff_users is null or max_staff_users > 0) and
            (max_products is null or max_products > 0)
        )
);

-- =========================================================
-- 12) SUBSCRIPTIONS TENANT
-- =========================================================
create table tenant_subscriptions (
    id                          bigserial primary key,
    tenant_id                   bigint not null references tenants(id) on delete cascade,
    plan_code                   varchar(50) not null references subscription_plans(code), 
    status                      varchar(30) not null default 'PENDING',
    billing_cycle               varchar(20) not null default 'MONTHLY',
    payment_provider            varchar(50),
    provider_customer_id        varchar(255),
    provider_subscription_id    varchar(255),
    payment_status              varchar(30) not null default 'PENDING',
    current_period_start        timestamptz,
    current_period_end          timestamptz,
    trial_ends_at               timestamptz,
    activated_at                timestamptz,
    cancelled_at                timestamptz,
    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz not null default now(),

    constraint chk_tenant_subscriptions_status
        check (status in ('PENDING', 'TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED')),

    constraint chk_tenant_subscriptions_billing_cycle
        check (billing_cycle in ('MONTHLY', 'YEARLY')),

    constraint chk_tenant_subscriptions_payment_status
        check (payment_status in ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'NONE'))
);

create unique index uq_tenant_subscriptions_current
    on tenant_subscriptions(tenant_id)
    where status in ('PENDING', 'TRIAL', 'ACTIVE', 'PAST_DUE');


-- =========================================================
-- 13) ORDINI
-- =========================================================
create table orders (
    id                      bigserial primary key,
    tenant_id               bigint not null references tenants(id) on delete cascade,
    location_id             bigint not null references locations(id) on delete restrict,
    location_label_snapshot varchar(150) not null,
    area_name_snapshot      varchar(150),
    source                  varchar(20) not null default 'QR',
    status                  varchar(30) not null default 'NEW',
    payment_status          varchar(30) not null default 'NONE',
    customer_note           varchar(500),
    internal_note           varchar(500),
    subtotal_amount         dom_price not null default 0,
    total_amount            dom_price not null default 0,
    created_by_staff_id     bigint null references staff_users(id) on delete set null,
    accepted_by_staff_id    bigint null references staff_users(id) on delete set null,
    delivered_by_staff_id   bigint null references staff_users(id) on delete set null,
    accepted_at             timestamptz,
    ready_at                timestamptz,
    delivered_at            timestamptz,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now(),

    constraint chk_orders_source
        check (source in ('QR', 'STAFF')),

    constraint chk_orders_status
        check (status in ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED')),

    constraint chk_orders_payment_status
        check (payment_status in ('NONE', 'PENDING', 'PAID', 'FAILED', 'REFUNDED')),

    constraint chk_orders_total_ge_subtotal
        check (total_amount >= subtotal_amount),

    constraint chk_orders_timestamps
        check (
            (accepted_at is null or accepted_at >= created_at) and
            (ready_at is null or ready_at >= created_at) and
            (delivered_at is null or delivered_at >= created_at)
        )
);

create table order_items (
    id                          bigserial primary key,
    order_id                    bigint not null references orders(id) on delete cascade,
    tenant_id                   bigint not null references tenants(id) on delete cascade,
    tenant_product_id           bigint not null references tenant_products(id) on delete restrict,
    product_name_snapshot       varchar(200) not null,
    unit_price_snapshot         dom_price not null,
    quantity                    dom_qty not null,
    line_total                  dom_price not null,
    department_snapshot         varchar(30) not null,
    notes                       varchar(300),
    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz not null default now(),

    constraint chk_order_items_department
        check (department_snapshot in ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),

    constraint chk_order_items_line_total
        check (line_total = round((unit_price_snapshot * quantity)::numeric, 2))
);

create table order_item_modifier_options (
    order_item_id                 bigint not null references order_items(id) on delete cascade,
    modifier_option_id            bigint not null references modifier_options(id) on delete restrict,
    modifier_group_name_snapshot  varchar(150) not null,
    option_name_snapshot          varchar(150) not null,
    price_delta_snapshot          dom_price not null default 0,
    primary key (order_item_id, modifier_option_id)
);

create table order_status_history (
    id              bigserial primary key,
    order_id        bigint not null references orders(id) on delete cascade,
    old_status      varchar(30),
    new_status      varchar(30) not null,
    changed_by      bigint null references staff_users(id) on delete set null,
    changed_at      timestamptz not null default now(),

    constraint chk_order_status_history_old
        check (old_status is null or old_status in ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED')),

    constraint chk_order_status_history_new
        check (new_status in ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED'))
);

-- =========================================================
-- 14) STORICO STATO TENANT
-- =========================================================
create table tenant_status_history (
    id                          bigserial primary key,
    tenant_id                   bigint not null references tenants(id) on delete cascade,
    old_status                  varchar(30),
    new_status                  varchar(30) not null,
    changed_by_staff_user_id    bigint null references staff_users(id) on delete set null,
    reason                      varchar(500),
    changed_at                  timestamptz not null default now(),

    constraint chk_tenant_status_history_old
        check (old_status is null or old_status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED')),

    constraint chk_tenant_status_history_new
        check (new_status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED'))
);

-- =========================================================
-- 15) INDICI
-- =========================================================
create index idx_tenants_status on tenants(status);
create index idx_tenants_business_email on tenants(lower(business_email));

create index idx_areas_tenant on areas(tenant_id);
create index idx_locations_tenant on locations(tenant_id);
create index idx_locations_area on locations(area_id);
create index idx_location_tokens_tenant on location_tokens(tenant_id);
create index idx_location_tokens_location on location_tokens(location_id);

create index idx_global_products_status on global_products(status);
create index idx_tenant_products_tenant on tenant_products(tenant_id);
create index idx_tenant_products_global on tenant_products(global_product_id);
create index idx_tenant_products_tenant_status on tenant_products(tenant_id, status, available_for_order);
create index idx_category_tenant_products_product on category_tenant_products(tenant_product_id);

create index idx_modifier_groups_tenant on modifier_groups(tenant_id);
create index idx_modifier_options_tenant on modifier_options(tenant_id);

create index idx_staff_users_tenant on staff_users(tenant_id);
create index idx_staff_users_primary_contact on staff_users(tenant_id, is_primary_contact);

create unique index uq_business_registration_requested_slug
    on business_registration_requests(lower(requested_slug))
    where status in ('SUBMITTED', 'APPROVED');

create index idx_business_registration_status
    on business_registration_requests(status);

create index idx_business_registration_contact_email
    on business_registration_requests(lower(contact_email));

create index idx_tenant_subscriptions_tenant
    on tenant_subscriptions(tenant_id);

create index idx_tenant_subscriptions_status
    on tenant_subscriptions(status);

create index idx_tenant_status_history_tenant
    on tenant_status_history(tenant_id, changed_at desc);

create index idx_orders_tenant_status_created on orders(tenant_id, status, created_at desc);
create index idx_orders_location on orders(location_id);
create index idx_order_items_order on order_items(order_id);
create index idx_order_items_tenant_product on order_items(tenant_product_id);

-- =========================================================
-- 16) FUNZIONI GENERICHE
-- =========================================================
create or replace function fn_set_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

create or replace function fn_generate_location_token()
returns trigger
language plpgsql
as $$
begin
    if new.token is null or length(trim(new.token)) = 0 then
        new.token := encode(gen_random_bytes(16), 'hex');
    end if;
    return new;
end;
$$;

-- =========================================================
-- 17) FUNZIONI DI COERENZA TENANT
-- =========================================================
create or replace function fn_check_location_area_tenant()
returns trigger
language plpgsql
as $$
declare
    v_area_tenant bigint;
begin
    if new.area_id is not null then
        select tenant_id into v_area_tenant
        from areas
        where id = new.area_id;

        if v_area_tenant is null then
            raise exception 'Area % inesistente', new.area_id;
        end if;

        if v_area_tenant <> new.tenant_id then
            raise exception 'La area % non appartiene al tenant %', new.area_id, new.tenant_id;
        end if;
    end if;

    return new;
end;
$$;

create or replace function fn_check_location_token_tenant()
returns trigger
language plpgsql
as $$
declare
    v_location_tenant bigint;
begin
    select tenant_id into v_location_tenant
    from locations
    where id = new.location_id;

    if v_location_tenant is null then
        raise exception 'Location % inesistente', new.location_id;
    end if;

    if v_location_tenant <> new.tenant_id then
        raise exception 'Location % non appartiene al tenant %', new.location_id, new.tenant_id;
    end if;

    return new;
end;
$$;

create or replace function fn_check_category_tenant_product_tenant()
returns trigger
language plpgsql
as $$
declare
    v_cat_tenant bigint;
    v_prod_tenant bigint;
begin
    select tenant_id into v_cat_tenant from categories where id = new.category_id;
    select tenant_id into v_prod_tenant from tenant_products where id = new.tenant_product_id;

    if v_cat_tenant is null or v_prod_tenant is null then
        raise exception 'Categoria o tenant_product inesistente';
    end if;

    if v_cat_tenant <> v_prod_tenant then
        raise exception 'Categoria e tenant_product appartengono a tenant diversi';
    end if;

    return new;
end;
$$;

create or replace function fn_check_modifier_option_tenant()
returns trigger
language plpgsql
as $$
declare
    v_group_tenant bigint;
begin
    select tenant_id into v_group_tenant
    from modifier_groups
    where id = new.modifier_group_id;

    if v_group_tenant is null then
        raise exception 'Modifier group % inesistente', new.modifier_group_id;
    end if;

    if v_group_tenant <> new.tenant_id then
        raise exception 'Modifier option e modifier group appartengono a tenant diversi';
    end if;

    return new;
end;
$$;

create or replace function fn_check_tenant_product_modifier_group_tenant()
returns trigger
language plpgsql
as $$
declare
    v_prod_tenant bigint;
    v_group_tenant bigint;
begin
    select tenant_id into v_prod_tenant from tenant_products where id = new.tenant_product_id;
    select tenant_id into v_group_tenant from modifier_groups where id = new.modifier_group_id;

    if v_prod_tenant is null or v_group_tenant is null then
        raise exception 'Tenant product o gruppo modificatori inesistente';
    end if;

    if v_prod_tenant <> v_group_tenant then
        raise exception 'Tenant product e gruppo modificatori appartengono a tenant diversi';
    end if;

    return new;
end;
$$;

create or replace function fn_check_order_tenant_and_snapshots()
returns trigger
language plpgsql
as $$
declare
    v_location_tenant bigint;
    v_location_label varchar(150);
    v_area_name varchar(150);
begin
    select l.tenant_id, l.label, a.name
      into v_location_tenant, v_location_label, v_area_name
    from locations l
    left join areas a on a.id = l.area_id
    where l.id = new.location_id;

    if v_location_tenant is null then
        raise exception 'Location % inesistente', new.location_id;
    end if;

    if v_location_tenant <> new.tenant_id then
        raise exception 'La location % non appartiene al tenant %', new.location_id, new.tenant_id;
    end if;

    if tg_op = 'INSERT' then
        new.location_label_snapshot := v_location_label;
        new.area_name_snapshot := v_area_name;
    end if;

    return new;
end;
$$;

create or replace function fn_check_order_item_tenant_and_snapshots()
returns trigger
language plpgsql
as $$
declare
    v_order_tenant bigint;
    v_product_tenant bigint;
    v_product_name varchar(200);
    v_product_price numeric(10,2);
    v_product_department varchar(30);
begin
    select tenant_id into v_order_tenant
    from orders
    where id = new.order_id;

    select tenant_id, name, price, department
      into v_product_tenant, v_product_name, v_product_price, v_product_department
    from tenant_products
    where id = new.tenant_product_id;

    if v_order_tenant is null then
        raise exception 'Ordine % inesistente', new.order_id;
    end if;

    if v_product_tenant is null then
        raise exception 'Prodotto tenant % inesistente', new.tenant_product_id;
    end if;

    if new.tenant_id <> v_order_tenant then
        raise exception 'Order item con tenant incoerente rispetto all''ordine';
    end if;

    if new.tenant_id <> v_product_tenant then
        raise exception 'Order item con tenant incoerente rispetto al prodotto tenant';
    end if;

    if tg_op = 'INSERT' then
        new.product_name_snapshot := v_product_name;
        new.unit_price_snapshot := v_product_price;
        new.department_snapshot := v_product_department;
    end if;

    new.line_total := round((new.unit_price_snapshot * new.quantity)::numeric, 2);

    return new;
end;
$$;

create or replace function fn_check_order_item_modifier_option()
returns trigger
language plpgsql
as $$
declare
    v_order_tenant bigint;
    v_option_tenant bigint;
    v_group_name varchar(150);
    v_option_name varchar(150);
    v_price_delta numeric(10,2);
begin
    select o.tenant_id
      into v_order_tenant
    from orders o
    join order_items oi on oi.order_id = o.id
    where oi.id = new.order_item_id;

    select mo.tenant_id, mg.name, mo.name, mo.price_delta
      into v_option_tenant, v_group_name, v_option_name, v_price_delta
    from modifier_options mo
    join modifier_groups mg on mg.id = mo.modifier_group_id
    where mo.id = new.modifier_option_id;

    if v_option_tenant is null then
        raise exception 'Modifier option % inesistente', new.modifier_option_id;
    end if;

    if v_order_tenant <> v_option_tenant then
        raise exception 'Modifier option e order item appartengono a tenant diversi';
    end if;

    if tg_op = 'INSERT' then
        new.modifier_group_name_snapshot := v_group_name;
        new.option_name_snapshot := v_option_name;
        new.price_delta_snapshot := v_price_delta;
    end if;

    return new;
end;
$$;

-- =========================================================
-- 18) FUNZIONI BUSINESS
-- =========================================================
create or replace function fn_fill_tenant_product_from_global()
returns trigger
language plpgsql
as $$
declare
    v_name varchar(200);
    v_description varchar(500);
    v_image text;
    v_department varchar(30);
    v_vat numeric(5,2);
begin
    if new.global_product_id is not null then
        select name, description, default_image_url, default_department, default_vat_rate
          into v_name, v_description, v_image, v_department, v_vat
        from global_products
        where id = new.global_product_id and status = 'ACTIVE';

        if v_name is null then
            raise exception 'Global product % inesistente o non attivo', new.global_product_id;
        end if;

        new.name := coalesce(new.name, v_name);
        new.description := coalesce(new.description, v_description);
        new.image_url := coalesce(new.image_url, v_image);
        new.department := coalesce(new.department, v_department);
        new.vat_rate := coalesce(new.vat_rate, v_vat);
    end if;

    return new;
end;
$$;

create or replace function fn_recalculate_order_totals(p_order_id bigint)
returns void
language plpgsql
as $$
declare
    v_items_total numeric(10,2);
    v_modifiers_total numeric(10,2);
begin
    select coalesce(sum(line_total), 0)
      into v_items_total
    from order_items
    where order_id = p_order_id;

    select coalesce(sum(oi.quantity * oimo.price_delta_snapshot), 0)
      into v_modifiers_total
    from order_items oi
    join order_item_modifier_options oimo on oimo.order_item_id = oi.id
    where oi.order_id = p_order_id;

    update orders
       set subtotal_amount = round(v_items_total + v_modifiers_total, 2),
           total_amount    = round(v_items_total + v_modifiers_total, 2),
           updated_at      = now()
     where id = p_order_id;
end;
$$;

create or replace function fn_order_totals_trigger()
returns trigger
language plpgsql
as $$
declare
    v_order_id bigint;
begin
    v_order_id := coalesce(new.order_id, old.order_id);
    perform fn_recalculate_order_totals(v_order_id);
    return null;
end;
$$;

create or replace function fn_prevent_edit_closed_orders()
returns trigger
language plpgsql
as $$
declare
    v_status varchar(30);
    v_order_id bigint;
begin
    if tg_table_name = 'order_item_modifier_options' then
        select oi.order_id
          into v_order_id
        from order_items oi
        where oi.id = coalesce(new.order_item_id, old.order_item_id);
    else
        v_order_id := coalesce(new.order_id, old.order_id);
    end if;

    select status into v_status
    from orders
    where id = v_order_id;

    if v_status in ('DELIVERED', 'CANCELLED') then
        raise exception 'Impossibile modificare righe di un ordine con stato %', v_status;
    end if;

    return coalesce(new, old);
end;
$$;

-- MVP: per ora solo NEW -> DELIVERED / CANCELLED
create or replace function fn_validate_order_status_transition()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'UPDATE' and old.status <> new.status then
        if old.status = 'NEW' and new.status not in ('DELIVERED', 'CANCELLED') then
            raise exception 'Transizione non valida da NEW a %', new.status;
        elsif old.status in ('DELIVERED', 'CANCELLED') and new.status <> old.status then
            raise exception 'Ordine finale: impossibile cambiare stato da % a %', old.status, new.status;
        end if;

        if new.status = 'DELIVERED' and new.delivered_at is null then
            new.delivered_at := now();
        end if;
    end if;

    return new;
end;
$$;

create or replace function fn_insert_order_status_history()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'INSERT' then
        insert into order_status_history(order_id, old_status, new_status, changed_by, changed_at)
        values (new.id, null, new.status, new.created_by_staff_id, now());
    elsif tg_op = 'UPDATE' and old.status <> new.status then
        insert into order_status_history(order_id, old_status, new_status, changed_by, changed_at)
        values (
            new.id,
            old.status,
            new.status,
            coalesce(new.delivered_by_staff_id, new.created_by_staff_id),
            now()
        );
    end if;

    return new;
end;
$$;

create or replace function fn_insert_tenant_status_history()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'INSERT' then
        insert into tenant_status_history(
            tenant_id,
            old_status,
            new_status,
            changed_by_staff_user_id,
            reason,
            changed_at
        )
        values (
            new.id,
            null,
            new.status,
            new.approved_by_staff_user_id,
            'Tenant created',
            now()
        );

    elsif tg_op = 'UPDATE' and old.status <> new.status then
        insert into tenant_status_history(
            tenant_id,
            old_status,
            new_status,
            changed_by_staff_user_id,
            reason,
            changed_at
        )
        values (
            new.id,
            old.status,
            new.status,
            new.approved_by_staff_user_id,
            'Tenant status changed',
            now()
        );
    end if;

    return new;
end;
$$;

create or replace function fn_validate_tenant_activation()
returns trigger
language plpgsql
as $$
begin
    if tg_op = 'UPDATE' and old.status <> new.status then
        if new.status = 'ACTIVE' then
            if new.activation_date is null then
                new.activation_date := now();
            end if;

            if new.approved_at is null then
                new.approved_at := now();
            end if;
        end if;

        if old.status = 'DISABLED' and new.status <> 'DISABLED' then
            raise exception 'Tenant DISABLED non può essere riattivato automaticamente';
        end if;
    end if;

    return new;
end;
$$;

-- =========================================================
-- 19) TRIGGERS
-- =========================================================
create trigger trg_tenants_updated_at
before update on tenants
for each row execute function fn_set_updated_at();

create trigger trg_areas_updated_at
before update on areas
for each row execute function fn_set_updated_at();

create trigger trg_locations_updated_at
before update on locations
for each row execute function fn_set_updated_at();

create trigger trg_location_tokens_updated_at
before update on location_tokens
for each row execute function fn_set_updated_at();

create trigger trg_categories_updated_at
before update on categories
for each row execute function fn_set_updated_at();

create trigger trg_global_categories_updated_at
before update on global_categories
for each row execute function fn_set_updated_at();

create trigger trg_global_products_updated_at
before update on global_products
for each row execute function fn_set_updated_at();

create trigger trg_tenant_products_updated_at
before update on tenant_products
for each row execute function fn_set_updated_at();

create trigger trg_modifier_groups_updated_at
before update on modifier_groups
for each row execute function fn_set_updated_at();

create trigger trg_modifier_options_updated_at
before update on modifier_options
for each row execute function fn_set_updated_at();

create trigger trg_staff_users_updated_at
before update on staff_users
for each row execute function fn_set_updated_at();

create trigger trg_business_registration_requests_updated_at
before update on business_registration_requests
for each row execute function fn_set_updated_at();

create trigger trg_tenant_subscriptions_updated_at
before update on tenant_subscriptions
for each row execute function fn_set_updated_at();

create trigger trg_orders_updated_at
before update on orders
for each row execute function fn_set_updated_at();

create trigger trg_order_items_updated_at
before update on order_items
for each row execute function fn_set_updated_at();

create trigger trg_location_tokens_generate
before insert on location_tokens
for each row execute function fn_generate_location_token();

create trigger trg_locations_area_tenant
before insert or update on locations
for each row execute function fn_check_location_area_tenant();

create trigger trg_location_tokens_tenant
before insert or update on location_tokens
for each row execute function fn_check_location_token_tenant();

create trigger trg_category_tenant_products_tenant
before insert or update on category_tenant_products
for each row execute function fn_check_category_tenant_product_tenant();

create trigger trg_modifier_options_tenant
before insert or update on modifier_options
for each row execute function fn_check_modifier_option_tenant();

create trigger trg_tenant_product_modifier_groups_tenant
before insert or update on tenant_product_modifier_groups
for each row execute function fn_check_tenant_product_modifier_group_tenant();

create trigger trg_orders_tenant
before insert or update on orders
for each row execute function fn_check_order_tenant_and_snapshots();

create trigger trg_order_items_tenant
before insert or update on order_items
for each row execute function fn_check_order_item_tenant_and_snapshots();

create trigger trg_order_item_modifier_options_tenant
before insert or update on order_item_modifier_options
for each row execute function fn_check_order_item_modifier_option();

create trigger trg_tenant_products_fill_from_global
before insert on tenant_products
for each row execute function fn_fill_tenant_product_from_global();

create trigger trg_order_items_prevent_closed_edit_ins
before insert on order_items
for each row execute function fn_prevent_edit_closed_orders();

create trigger trg_order_items_prevent_closed_edit_upd
before update on order_items
for each row execute function fn_prevent_edit_closed_orders();

create trigger trg_order_items_prevent_closed_edit_del
before delete on order_items
for each row execute function fn_prevent_edit_closed_orders();

create trigger trg_order_item_mod_prevent_closed_edit_ins
before insert on order_item_modifier_options
for each row execute function fn_prevent_edit_closed_orders();

create trigger trg_order_item_mod_prevent_closed_edit_upd
before update on order_item_modifier_options
for each row execute function fn_prevent_edit_closed_orders();

create trigger trg_order_item_mod_prevent_closed_edit_del
before delete on order_item_modifier_options
for each row execute function fn_prevent_edit_closed_orders();

create trigger trg_tenants_validate_activation
before update on tenants
for each row execute function fn_validate_tenant_activation();

create trigger trg_tenants_status_history
after insert or update on tenants
for each row execute function fn_insert_tenant_status_history();

create trigger trg_orders_validate_status_transition
before update on orders
for each row execute function fn_validate_order_status_transition();

create trigger trg_orders_status_history
after insert or update on orders
for each row execute function fn_insert_order_status_history();

create trigger trg_order_items_recalc_ins
after insert on order_items
for each row execute function fn_order_totals_trigger();

create trigger trg_order_items_recalc_upd
after update on order_items
for each row execute function fn_order_totals_trigger();

create trigger trg_order_items_recalc_del
after delete on order_items
for each row execute function fn_order_totals_trigger();

create trigger trg_order_item_mod_recalc_ins
after insert on order_item_modifier_options
for each row execute function fn_order_totals_trigger();

create trigger trg_order_item_mod_recalc_upd
after update on order_item_modifier_options
for each row execute function fn_order_totals_trigger();

create trigger trg_order_item_mod_recalc_del
after delete on order_item_modifier_options
for each row execute function fn_order_totals_trigger();

-- =========================================================
-- 20) RUOLI BASE
-- =========================================================
insert into staff_roles(code, description) values
('MANAGER', 'Referente o gestore del tenant'),
('BAR', 'Operatore bar'),
('KITCHEN', 'Operatore cucina'),
('RUNNER', 'Addetto consegna');