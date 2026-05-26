package com.orderapp.ordering.multitenant;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.orderapp.ordering.entity.Tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(TenantResolutionFilter.class);

	private final TenantResolverService tenantResolverService;
	private final ObjectMapper objectMapper;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.startsWith("/api/public/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		// If this is an admin endpoint or the authenticated user is super-admin, skip tenant resolution
		String requestUri = request.getRequestURI();
		if (requestUri != null && (requestUri.startsWith("/api/admin") || requestUri.startsWith("/admin"))) {
			filterChain.doFilter(request, response);
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getAuthorities() != null) {
			boolean isSuperAdmin = authentication.getAuthorities().stream()
					.anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
			if (isSuperAdmin) {
				filterChain.doFilter(request, response);
				return;
			}
		}

		try {
			String host = request.getHeader(HttpHeaders.HOST);
			String subdomain = extractSubdomain(host);
			String tenantIdHeader = request.getHeader("X-Tenant-Id");

			log.info("[tenant-filter] Resolving tenant for request method={} uri={} host={} subdomain={} xTenantId={}",
				request.getMethod(),
				request.getRequestURI(),
				host,
				subdomain,
				tenantIdHeader);

			Tenant tenant;
			if (subdomain != null && !subdomain.isBlank()) {
				tenant = tenantResolverService.resolveBySubdomainOrThrow(subdomain);
				log.info("[tenant-filter] Resolved by subdomain tenantId={} tenantSlug={}", tenant.getId(), tenant.getSubdomain());
			} else {
				// Check for explicit tenant query parameter (used by SockJS connections from the frontend)
				String tenantParam = request.getParameter("tenant");
				if (tenantParam != null && !tenantParam.isBlank()) {
					tenant = tenantResolverService.resolveBySubdomainOrThrow(tenantParam);
					log.info("[tenant-filter] Resolved by tenant query param tenantSlug={}", tenantParam);
				} else {
					// Support xTenantId query param as an alternative to X-Tenant-Id header
					String xTenantIdParam = request.getParameter("xTenantId");
					if (xTenantIdParam != null && !xTenantIdParam.isBlank()) {
						try {
							Long tenantId = Long.parseLong(xTenantIdParam);
							tenant = tenantResolverService.resolveByIdOrThrow(tenantId);
							log.info("[tenant-filter] Resolved by xTenantId query param tenantId={}", tenantId);
						} catch (NumberFormatException ex) {
							throw new TenantNotResolvedException("Invalid xTenantId query parameter: " + xTenantIdParam);
						}
					} else {
						if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
							throw new TenantNotResolvedException("Missing tenant subdomain in Host header");
						}
						Long tenantId;
						try {
							tenantId = Long.parseLong(tenantIdHeader);
						} catch (NumberFormatException ex) {
							throw new TenantNotResolvedException("Invalid X-Tenant-Id header value: " + tenantIdHeader);
						}
						tenant = tenantResolverService.resolveByIdOrThrow(tenantId);
						log.info("[tenant-filter] Resolved by X-Tenant-Id tenantId={} tenantSlug={}", tenant.getId(), tenant.getSubdomain());
					}
				}
			}

			TenantContext.setTenant(tenant.getId(), tenant.getSubdomain());

			filterChain.doFilter(request, response);
		} catch (TenantNotResolvedException ex) {
			log.warn("[tenant-filter] Tenant resolution failed method={} uri={} message={}",
				request.getMethod(),
				request.getRequestURI(),
				ex.getMessage());

			response.setStatus(HttpStatus.BAD_REQUEST.value());
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(objectMapper.writeValueAsString(Map.of(
				"timestamp", OffsetDateTime.now().toString(),
				"status", HttpStatus.BAD_REQUEST.value(),
				"error", "TENANT_NOT_RESOLVED",
				"message", ex.getMessage(),
				"path", request.getRequestURI()
			)));
		} finally {
			TenantContext.clear();
		}
	}

	/**
	 * Expected formats:
	 * - tenant-slug.domain.com
	 * - tenant-slug.localhost:4200 (dev)
	 */
	static String extractSubdomain(String hostHeader) {
		if (hostHeader == null || hostHeader.isBlank()) {
			return null;
		}
		String host = hostHeader.trim().toLowerCase();
		int portIdx = host.indexOf(':');
		if (portIdx >= 0) {
			host = host.substring(0, portIdx);
		}

		// dev shortcut: tenant.localhost
		if (host.endsWith(".localhost")) {
			return host.substring(0, host.length() - ".localhost".length());
		}

		String[] parts = host.split("\\.");
		if (parts.length < 3) { // subdomain + domain + tld
			return null;
		}
		return parts[0];
	}
}

