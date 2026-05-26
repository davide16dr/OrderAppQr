-- ===== OPTIMIZED INDEXES FOR HIGH-FREQUENCY QUERIES =====
-- These indexes complement the existing ones for multi-tenant scenarios

-- ===== TENANTS PERFORMANCE =====
CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_tenants_subdomain ON tenants(subdomain);
CREATE INDEX IF NOT EXISTS idx_tenants_enabled_status ON tenants(enabled, status) WHERE enabled = true;
CREATE INDEX IF NOT EXISTS idx_tenants_created_desc ON tenants(created_at DESC);

-- ===== STAFF USERS PERFORMANCE =====
-- Multi-tenant staff user lookups
CREATE INDEX IF NOT EXISTS idx_staff_users_tenant_email ON staff_users(tenant_id, email);
CREATE INDEX IF NOT EXISTS idx_staff_users_tenant_status ON staff_users(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_staff_users_activated ON staff_users(tenant_id, activated_at) WHERE activated_at IS NOT NULL;

-- ===== LOCATIONS & AREAS PERFORMANCE =====
-- Frequently filtered by tenant and operational status (for customer menu)
CREATE INDEX IF NOT EXISTS idx_locations_tenant_status ON locations(tenant_id, status, operational_status);
CREATE INDEX IF NOT EXISTS idx_locations_label_ci ON locations(tenant_id, LOWER(label));
CREATE INDEX IF NOT EXISTS idx_areas_tenant_status ON areas(tenant_id, status);

-- ===== LOCATION TOKENS PERFORMANCE =====
-- QR scanning queries
CREATE INDEX IF NOT EXISTS idx_location_tokens_token ON location_tokens(token);
CREATE INDEX IF NOT EXISTS idx_location_tokens_tenant_location_primary ON location_tokens(tenant_id, location_id, is_primary) WHERE status = 'ACTIVE';

-- ===== CATEGORIES & PRODUCTS PERFORMANCE =====
-- Catalog browsing queries for customer menu
CREATE INDEX IF NOT EXISTS idx_categories_tenant_status_order ON categories(tenant_id, status, display_order);
CREATE INDEX IF NOT EXISTS idx_tenant_products_tenant_category ON tenant_products(tenant_id) WHERE status = 'ACTIVE' AND available_for_order = true;
CREATE INDEX IF NOT EXISTS idx_tenant_products_status_order ON tenant_products(tenant_id, status, available_for_order DESC, display_order);

-- ===== MODIFIER GROUPS & OPTIONS PERFORMANCE =====
-- Fetched with products during order creation
CREATE INDEX IF NOT EXISTS idx_modifier_groups_tenant_status ON modifier_groups(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_modifier_options_group_status ON modifier_options(modifier_group_id, status);

-- ===== ORDERS PERFORMANCE =====
-- Most critical for performance: filtering by date, status, location
CREATE INDEX IF NOT EXISTS idx_orders_tenant_location_created ON orders(tenant_id, location_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_location_status ON orders(location_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_status_created_desc ON orders(tenant_id, status, created_at DESC) WHERE status IN ('NEW', 'ACCEPTED', 'IN_PROGRESS', 'READY');
CREATE INDEX IF NOT EXISTS idx_orders_staff_user ON orders(tenant_id, accepted_by_staff_id, created_at DESC) WHERE accepted_by_staff_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_orders_date_range ON orders(tenant_id, created_at) WHERE created_at >= CURRENT_DATE - INTERVAL '30 days';

-- ===== ORDER ITEMS PERFORMANCE =====
-- Fetched with orders and location details
CREATE INDEX IF NOT EXISTS idx_order_items_order_created ON order_items(order_id, created_at);

-- ===== BUSINESS REGISTRATION PERFORMANCE =====
-- Filtering pending registrations for admin approval
CREATE INDEX IF NOT EXISTS idx_business_registration_status_created ON business_registration_requests(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_business_registration_tenant ON business_registration_requests(tenant_id) WHERE status = 'CONVERTED';

-- ===== SUBSCRIPTIONS PERFORMANCE =====
-- Billing cycle queries and renewal checks
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant_active ON tenant_subscriptions(tenant_id, status) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_trial_ends ON tenant_subscriptions(trial_ends_at) WHERE status = 'TRIAL' AND trial_ends_at IS NOT NULL;

-- ===== HISTORY TABLES PERFORMANCE =====
-- Audit trail and analytics queries
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_created ON order_status_history(order_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS idx_tenant_status_history_tenant_created ON tenant_status_history(tenant_id, changed_at DESC);

-- ===== FULL-TEXT SEARCH INDEXES (Optional - for future enhancement) =====
-- Uncomment if you implement full-text search for products, categories, etc.
-- CREATE INDEX IF NOT EXISTS idx_products_name_search ON tenant_products USING GIN(to_tsvector('italian', name));
-- CREATE INDEX IF NOT EXISTS idx_categories_name_search ON categories USING GIN(to_tsvector('italian', name));

-- ===== COMPOSITE INDEXES FOR COMMON JOINS =====
-- tenant -> locations -> areas for menu building
CREATE INDEX IF NOT EXISTS idx_locations_area_tenant ON locations(area_id, tenant_id) WHERE status = 'ACTIVE';

-- tenant_products -> categories -> modifiers for order creation
CREATE INDEX IF NOT EXISTS idx_category_products_category ON category_tenant_products(category_id);

ANALYZE;
