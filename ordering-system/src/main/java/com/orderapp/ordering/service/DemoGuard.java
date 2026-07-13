package com.orderapp.ordering.service;

import com.orderapp.ordering.exception.DemoModeException;
import com.orderapp.ordering.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoGuard {

    private final TenantRepository tenantRepository;

    public void checkNotDemo(Long tenantId) {
        if (tenantId == null) return;
        tenantRepository.findById(tenantId)
                .filter(t -> t.isDemo())
                .ifPresent(t -> { throw new DemoModeException(); });
    }
}
