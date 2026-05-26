package com.orderapp.ordering.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderapp.ordering.dto.AreaCreateRequest;
import com.orderapp.ordering.dto.AreaResponse;
import com.orderapp.ordering.dto.AreaUpdateRequest;
import com.orderapp.ordering.multitenant.TenantContext;
import com.orderapp.ordering.service.AreaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff/areas")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
public class StaffAreaController {
    private final AreaService areaService;

    @GetMapping
    public List<AreaResponse> getAreas() {
        return areaService.listAreasByTenant(TenantContext.getTenantId());
    }

    @GetMapping("/{areaId}")
    public AreaResponse getArea(@PathVariable Long areaId) {
        return areaService.getAreaById(TenantContext.getTenantId(), areaId);
    }

    @PostMapping
    public ResponseEntity<AreaResponse> createArea(@Valid @RequestBody AreaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(areaService.createArea(TenantContext.getTenantId(), request));
    }

    @PutMapping("/{areaId}")
    public AreaResponse updateArea(@PathVariable Long areaId, @Valid @RequestBody AreaUpdateRequest request) {
        return areaService.updateArea(TenantContext.getTenantId(), areaId, request);
    }

    @DeleteMapping("/{areaId}")
    public ResponseEntity<Void> deleteArea(@PathVariable Long areaId) {
        areaService.deleteArea(TenantContext.getTenantId(), areaId);
        return ResponseEntity.noContent().build();
    }
}
