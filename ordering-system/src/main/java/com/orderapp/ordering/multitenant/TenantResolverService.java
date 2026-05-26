package com.orderapp.ordering.multitenant;

import org.springframework.stereotype.Service;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantResolverService {
	private final TenantRepository tenantRepository;

	public Tenant resolveBySubdomainOrThrow(String subdomain) {
		return tenantRepository.findBySubdomainIgnoreCase(subdomain)
			.filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
			.orElseThrow(() -> new TenantNotResolvedException("Tenant not found or not active for subdomain: " + subdomain));
	}

	public Tenant resolveByIdOrThrow(Long tenantId) {
		return tenantRepository.findById(tenantId)
			.orElseThrow(() -> new TenantNotResolvedException("Tenant not found for id: " + tenantId));
	}
}

