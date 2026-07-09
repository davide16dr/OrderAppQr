drop table if exists order_item_modifier_options;
drop table if exists order_status_history;
drop table if exists order_items;
drop table if exists orders;
drop table if exists tenant_status_history;

drop table if exists tenant_subscriptions;
drop table if exists subscription_plans;

alter table if exists tenants drop constraint if exists fk_tenants_approved_by_staff;

drop table if exists staff_user_roles;
drop table if exists staff_roles;
drop table if exists staff_users;

drop table if exists tenant_product_modifier_groups;
drop table if exists modifier_options;
drop table if exists modifier_groups;
drop table if exists category_tenant_products;
drop table if exists tenant_products;
drop table if exists global_category_products;
drop table if exists global_products;
drop table if exists global_categories;
drop table if exists categories;

drop table if exists location_tokens;
drop table if exists locations;
drop table if exists areas;

drop table if exists business_registration_requests;
drop table if exists tenants;

drop sequence if exists tenants_id_seq;
create sequence tenants_id_seq start with 100 increment by 1;

create table tenants (
  id bigserial primary key,
  slug varchar(100) not null unique,
  name varchar(255) not null,
  legal_name varchar(255),
  business_type varchar(30) not null,
  status varchar(30) not null default 'PENDING',
  enabled boolean not null default false,
  timezone varchar(100) not null default 'Europe/Rome',
  currency_code char(3) not null default 'EUR',
  subdomain varchar(100) not null unique,
  vat_number varchar(50),
  business_email varchar(255),
  business_phone varchar(50),
  address_line_1 varchar(255),
  address_line_2 varchar(255),
  city varchar(100),
  province varchar(100),
  postal_code varchar(20),
  country varchar(100) not null default 'Italy',
  registration_source varchar(30) not null default 'SELF_SIGNUP',
  activation_date timestamp,
  approved_at timestamp,
  approved_by_staff_user_id bigint,
  branding_json varchar(4000) default '{}',
  opening_config_json varchar(4000) default '{}',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_tenants_business_type check (business_type in ('LIDO', 'BAR', 'RESTAURANT', 'NIGHTCLUB', 'OTHER')),
  constraint chk_tenants_status check (status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED')),
  constraint chk_tenants_registration_source check (registration_source in ('SELF_SIGNUP', 'INTERNAL_CREATE', 'SEEDER')),
  constraint chk_tenants_currency check (currency_code in ('EUR')),
  constraint chk_tenants_country check (country in ('Italy', 'IT'))
);

create table areas (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  name varchar(150) not null,
  description varchar(300),
  display_order integer not null default 0,
  status varchar(30) not null default 'ACTIVE',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_areas_status check (status in ('ACTIVE', 'DISABLED')),
  constraint uq_areas_tenant_name unique (tenant_id, name)
);

create table locations (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  area_id bigint references areas(id) on delete set null,
  type varchar(30) not null,
  label varchar(150) not null,
  status varchar(30) not null default 'ACTIVE',
  operational_status varchar(30) not null default 'AVAILABLE',
  capacity integer,
  notes varchar(500),
  metadata_json varchar(4000) not null default '{}',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_locations_type check (type in ('TABLE', 'UMBRELLA', 'SUNBED', 'VIP', 'ROOM', 'LOUNGE', 'GENERIC')),
  constraint chk_locations_status check (status in ('ACTIVE', 'DISABLED')),
  constraint chk_locations_operational_status check (operational_status in ('AVAILABLE', 'OCCUPIED', 'RESERVED', 'ORDERING_DISABLED', 'CLOSED')),
  constraint chk_locations_capacity check (capacity is null or capacity > 0),
  constraint uq_locations_tenant_label unique (tenant_id, label)
);

create table location_tokens (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  location_id bigint not null references locations(id) on delete cascade,
  token varchar(120) not null unique,
  status varchar(30) not null default 'ACTIVE',
  is_primary boolean not null default true,
  rotatable boolean not null default true,
  expires_at timestamp,
  qr_value varchar(4000) not null default '',
  image_path varchar(1000),
  generated_at timestamp not null default current_timestamp,
  regenerated_at timestamp,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_location_tokens_status check (status in ('ACTIVE', 'REVOKED', 'EXPIRED')),
  constraint chk_location_tokens_expiration check (expires_at is null or expires_at > created_at)
);

create table categories (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  name varchar(150) not null,
  description varchar(300),
  display_order integer not null default 0,
  status varchar(30) not null default 'ACTIVE',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_categories_status check (status in ('ACTIVE', 'DISABLED')),
  constraint uq_categories_tenant_name unique (tenant_id, name)
);

create table global_categories (
  id bigserial primary key,
  code varchar(100) not null unique,
  name varchar(150) not null,
  description varchar(300),
  display_order integer not null default 0,
  status varchar(30) not null default 'ACTIVE',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_global_categories_status check (status in ('ACTIVE', 'DISABLED'))
);

create table global_products (
  id bigserial primary key,
  code varchar(100) not null unique,
  name varchar(200) not null,
  description varchar(500),
  default_image_url text,
  default_department varchar(30) not null default 'BAR',
  default_vat_rate numeric(5,2) not null default 10.00,
  status varchar(30) not null default 'ACTIVE',
  metadata_json varchar(4000) not null default '{}',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_global_products_department check (default_department in ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),
  constraint chk_global_products_status check (status in ('ACTIVE', 'DISABLED')),
  constraint chk_global_products_vat_rate check (default_vat_rate >= 0 and default_vat_rate <= 100)
);

create table global_category_products (
  global_category_id bigint not null references global_categories(id) on delete cascade,
  global_product_id bigint not null references global_products(id) on delete cascade,
  display_order integer not null default 0,
  primary key (global_category_id, global_product_id)
);

create table tenant_products (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  global_product_id bigint references global_products(id) on delete set null,
  sku varchar(50),
  name varchar(200) not null,
  description varchar(500),
  price numeric(10,2) not null,
  image_url text,
  department varchar(30) not null default 'BAR',
  vat_rate numeric(5,2) not null default 10.00,
  status varchar(30) not null default 'ACTIVE',
  available_for_order boolean not null default true,
  is_customized boolean not null default false,
  metadata_json varchar(4000) not null default '{}',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_tenant_products_department check (department in ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),
  constraint chk_tenant_products_status check (status in ('ACTIVE', 'DISABLED')),
  constraint chk_tenant_products_vat_rate check (vat_rate >= 0 and vat_rate <= 100),
  constraint uq_tenant_products_tenant_sku unique (tenant_id, sku)
);

create table category_tenant_products (
  category_id bigint not null references categories(id) on delete cascade,
  tenant_product_id bigint not null references tenant_products(id) on delete cascade,
  display_order integer not null default 0,
  primary key (category_id, tenant_product_id)
);

create table modifier_groups (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  name varchar(150) not null,
  min_selectable integer not null default 0,
  max_selectable integer,
  required boolean not null default false,
  status varchar(30) not null default 'ACTIVE',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_modifier_groups_status check (status in ('ACTIVE', 'DISABLED')),
  constraint chk_modifier_groups_min check (min_selectable >= 0),
  constraint chk_modifier_groups_max check (max_selectable is null or max_selectable >= min_selectable),
  constraint uq_modifier_groups_tenant_name unique (tenant_id, name)
);

create table modifier_options (
  id bigserial primary key,
  modifier_group_id bigint not null references modifier_groups(id) on delete cascade,
  tenant_id bigint not null references tenants(id) on delete cascade,
  name varchar(150) not null,
  price_delta numeric(10,2) not null default 0,
  status varchar(30) not null default 'ACTIVE',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_modifier_options_status check (status in ('ACTIVE', 'DISABLED')),
  constraint uq_modifier_options_group_name unique (modifier_group_id, name)
);

create table tenant_product_modifier_groups (
  tenant_product_id bigint not null references tenant_products(id) on delete cascade,
  modifier_group_id bigint not null references modifier_groups(id) on delete cascade,
  primary key (tenant_product_id, modifier_group_id)
);

create table staff_users (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  first_name varchar(100) not null,
  last_name varchar(100) not null,
  email varchar(255) not null,
  password_hash varchar(255) not null,
  phone varchar(50),
  is_primary_contact boolean not null default false,
  invited_at timestamp,
  activated_at timestamp,
  last_login_at timestamp,
  status varchar(30) not null default 'ACTIVE',
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_staff_users_status check (status in ('ACTIVE', 'DISABLED'))
);

create table staff_roles (
  id bigserial primary key,
  code varchar(50) not null unique,
  description varchar(255)
);

create table staff_user_roles (
  staff_user_id bigint not null references staff_users(id) on delete cascade,
  role_id bigint not null references staff_roles(id) on delete cascade,
  primary key (staff_user_id, role_id)
);

alter table tenants
  add constraint fk_tenants_approved_by_staff
  foreign key (approved_by_staff_user_id)
  references staff_users(id)
  on delete set null;

create table business_registration_requests (
  id bigserial primary key,
  requested_slug varchar(100) not null,
  tenant_name varchar(255) not null,
  legal_name varchar(255),
  business_type varchar(30) not null,
  vat_number varchar(50),
  business_email varchar(255) not null,
  business_phone varchar(50),
  address_line_1 varchar(255),
  address_line_2 varchar(255),
  city varchar(100),
  province varchar(100),
  postal_code varchar(20),
  country varchar(100) not null default 'Italy',
  contact_first_name varchar(100) not null,
  contact_last_name varchar(100) not null,
  contact_email varchar(255) not null,
  contact_phone varchar(50),
  password_hash varchar(255) not null,
  requested_plan_code varchar(50),
  status varchar(30) not null default 'SUBMITTED',
  submitted_at timestamp not null default current_timestamp,
  reviewed_at timestamp,
  reviewed_by_staff_user_id bigint references staff_users(id) on delete set null,
  rejection_reason varchar(500),
  tenant_id bigint references tenants(id) on delete set null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_business_registration_business_type check (business_type in ('LIDO', 'BAR', 'RESTAURANT', 'NIGHTCLUB', 'OTHER')),
  constraint chk_business_registration_status check (status in ('SUBMITTED', 'APPROVED', 'REJECTED', 'CONVERTED'))
);

create table subscription_plans (
  code varchar(50) primary key,
  name varchar(100) not null,
  description varchar(300),
  price_monthly numeric(10,2),
  price_yearly numeric(10,2),
  max_locations integer,
  max_staff_users integer,
  max_products integer,
  qr_batch_enabled boolean not null default false,
  realtime_dashboard boolean not null default true,
  global_catalog_enabled boolean not null default true,
  is_active boolean not null default true,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_subscription_plans_prices check ((price_monthly is null or price_monthly >= 0) and (price_yearly is null or price_yearly >= 0)),
  constraint chk_subscription_plans_limits check ((max_locations is null or max_locations > 0) and (max_staff_users is null or max_staff_users > 0) and (max_products is null or max_products > 0))
);

create table tenant_subscriptions (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  plan_code varchar(50) not null references subscription_plans(code),
  status varchar(30) not null default 'PENDING',
  billing_cycle varchar(20) not null default 'MONTHLY',
  payment_provider varchar(50),
  provider_customer_id varchar(255),
  provider_subscription_id varchar(255),
  payment_status varchar(30) not null default 'PENDING',
  current_period_start timestamp,
  current_period_end timestamp,
  trial_ends_at timestamp,
  activated_at timestamp,
  cancelled_at timestamp,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_tenant_subscriptions_status check (status in ('PENDING', 'TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELLED', 'EXPIRED')),
  constraint chk_tenant_subscriptions_billing_cycle check (billing_cycle in ('MONTHLY', 'YEARLY')),
  constraint chk_tenant_subscriptions_payment_status check (payment_status in ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'NONE'))
);

create table orders (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  location_id bigint not null references locations(id) on delete restrict,
  location_label_snapshot varchar(150) not null,
  area_name_snapshot varchar(150),
  source varchar(20) not null default 'QR',
  status varchar(30) not null default 'NEW',
  payment_status varchar(30) not null default 'NONE',
  customer_note varchar(500),
  internal_note varchar(500),
  subtotal_amount numeric(10,2) not null default 0,
  total_amount numeric(10,2) not null default 0,
  created_by_staff_id bigint references staff_users(id) on delete set null,
  accepted_by_staff_id bigint references staff_users(id) on delete set null,
  delivered_by_staff_id bigint references staff_users(id) on delete set null,
  accepted_at timestamp,
  ready_at timestamp,
  delivered_at timestamp,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  tenant_seq bigint not null default 0,
  constraint chk_orders_source check (source in ('QR', 'STAFF')),
  constraint chk_orders_status check (status in ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED')),
  constraint chk_orders_payment_status check (payment_status in ('NONE', 'PENDING', 'PAID', 'FAILED', 'REFUNDED')),
  constraint chk_orders_total_ge_subtotal check (total_amount >= subtotal_amount),
  constraint chk_orders_timestamps check ((accepted_at is null or accepted_at >= created_at) and (ready_at is null or ready_at >= created_at) and (delivered_at is null or delivered_at >= created_at))
);

create table order_items (
  id bigserial primary key,
  order_id bigint not null references orders(id) on delete cascade,
  tenant_id bigint not null references tenants(id) on delete cascade,
  tenant_product_id bigint not null references tenant_products(id) on delete restrict,
  product_name_snapshot varchar(200) not null,
  unit_price_snapshot numeric(10,2) not null,
  quantity integer not null,
  line_total numeric(10,2) not null,
  department_snapshot varchar(30) not null,
  notes varchar(300),
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp,
  constraint chk_order_items_department check (department_snapshot in ('BAR', 'KITCHEN', 'SERVICE', 'GENERIC')),
  constraint chk_order_items_qty check (quantity > 0),
  constraint chk_order_items_line_total check (line_total = round(unit_price_snapshot * quantity, 2))
);

create table order_item_modifier_options (
  order_item_id bigint not null references order_items(id) on delete cascade,
  modifier_option_id bigint not null references modifier_options(id) on delete restrict,
  modifier_group_name_snapshot varchar(150) not null,
  option_name_snapshot varchar(150) not null,
  price_delta_snapshot numeric(10,2) not null default 0,
  primary key (order_item_id, modifier_option_id)
);

create table order_status_history (
  id bigserial primary key,
  order_id bigint not null references orders(id) on delete cascade,
  old_status varchar(30),
  new_status varchar(30) not null,
  changed_by bigint references staff_users(id) on delete set null,
  changed_at timestamp not null default current_timestamp,
  constraint chk_order_status_history_old check (old_status is null or old_status in ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED')),
  constraint chk_order_status_history_new check (new_status in ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED'))
);

create table tenant_status_history (
  id bigserial primary key,
  tenant_id bigint not null references tenants(id) on delete cascade,
  old_status varchar(30),
  new_status varchar(30) not null,
  changed_by_staff_user_id bigint references staff_users(id) on delete set null,
  reason varchar(500),
  changed_at timestamp not null default current_timestamp,
  constraint chk_tenant_status_history_old check (old_status is null or old_status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED')),
  constraint chk_tenant_status_history_new check (new_status in ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED'))
);

create unique index uq_staff_users_tenant_email_ci on staff_users (tenant_id, email);
create unique index uq_location_tokens_active_primary on location_tokens (location_id, is_primary, status);
create unique index uq_tenant_subscriptions_current on tenant_subscriptions (tenant_id, status);
create unique index uq_business_registration_requested_slug on business_registration_requests (requested_slug, status);

create index idx_tenants_status on tenants(status);
create index idx_tenants_business_email on tenants(business_email);
create index idx_areas_tenant on areas(tenant_id);
create index idx_locations_tenant on locations(tenant_id);
create index idx_locations_area on locations(area_id);
create index idx_locations_tenant_operational_status on locations(tenant_id, operational_status);
create index idx_location_tokens_tenant on location_tokens(tenant_id);
create index idx_location_tokens_location on location_tokens(location_id);
create index idx_location_tokens_tenant_status on location_tokens(tenant_id, status);
create index idx_global_products_status on global_products(status);
create index idx_tenant_products_tenant on tenant_products(tenant_id);
create index idx_tenant_products_global on tenant_products(global_product_id);
create index idx_tenant_products_tenant_status on tenant_products(tenant_id, status, available_for_order);
create index idx_category_tenant_products_product on category_tenant_products(tenant_product_id);
create index idx_modifier_groups_tenant on modifier_groups(tenant_id);
create index idx_modifier_options_tenant on modifier_options(tenant_id);
create index idx_staff_users_tenant on staff_users(tenant_id);
create index idx_staff_users_primary_contact on staff_users(tenant_id, is_primary_contact);
create index idx_business_registration_status on business_registration_requests(status);
create index idx_business_registration_contact_email on business_registration_requests(contact_email);
create index idx_tenant_subscriptions_tenant on tenant_subscriptions(tenant_id);
create index idx_tenant_subscriptions_status on tenant_subscriptions(status);
create index idx_tenant_status_history_tenant on tenant_status_history(tenant_id, changed_at);
create index idx_orders_tenant_status_created on orders(tenant_id, status, created_at);
create index idx_orders_location on orders(location_id);
create index idx_order_items_order on order_items(order_id);
create index idx_order_items_tenant_product on order_items(tenant_product_id);
