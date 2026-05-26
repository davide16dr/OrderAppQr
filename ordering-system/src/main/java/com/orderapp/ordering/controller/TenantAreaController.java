package com.orderapp.ordering.controller;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderapp.ordering.dto.CreateTenantAreaRequest;
import com.orderapp.ordering.dto.TenantAreaDto;
import com.orderapp.ordering.dto.UpdateTenantAreaRequest;
import com.orderapp.ordering.multitenant.TenantContext;
import com.orderapp.ordering.repository.TenantAreaRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard/areas")
@RequiredArgsConstructor
public class TenantAreaController {
    private final TenantAreaRepository tenantAreaRepository;

    @GetMapping
    public List<TenantAreaDto> getAreas() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return List.of();
        }
        return tenantAreaRepository.findActiveAreas(tenantId);
    }

    @GetMapping("/{areaId}")
    public ResponseEntity<TenantAreaDto> getAreaById(@PathVariable long areaId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return tenantAreaRepository.findAreaById(tenantId, areaId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createArea(@Valid @RequestBody CreateTenantAreaRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            TenantAreaDto created = tenantAreaRepository.createArea(tenantId, request.name());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Area già esistente");
        }
    }

    @DeleteMapping("/{areaId}")
    public ResponseEntity<Void> disableArea(@PathVariable long areaId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean updated = tenantAreaRepository.disableArea(tenantId, areaId);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{areaId}")
    public ResponseEntity<?> updateArea(@PathVariable long areaId, @Valid @RequestBody UpdateTenantAreaRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            TenantAreaDto updated = tenantAreaRepository.updateArea(
                tenantId,
                areaId,
                request.name(),
                request.displayOrder(),
                request.status()
            );
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Area già esistente");
        }
    }
}
