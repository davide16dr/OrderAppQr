package com.orderapp.ordering.multitenant;

public final class TenantContext {
	private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
	private static final ThreadLocal<String> TENANT_SLUG = new ThreadLocal<>();

	private TenantContext() {
	}

	public static void setTenant(Long tenantId, String tenantSlug) {
		TENANT_ID.set(tenantId);
		TENANT_SLUG.set(tenantSlug);
	}

	public static Long getTenantId() {
		return TENANT_ID.get();
	}

	public static String getTenantSlug() {
		return TENANT_SLUG.get();
	}

	public static void clear() {
		TENANT_ID.remove();
		TENANT_SLUG.remove();
	}
}

