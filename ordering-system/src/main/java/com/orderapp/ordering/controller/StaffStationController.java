package com.orderapp.ordering.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orderapp.ordering.dto.StationCreateRequest;
import com.orderapp.ordering.dto.StationFilterRequest;
import com.orderapp.ordering.dto.StationQrResponse;
import com.orderapp.ordering.dto.StationResponse;
import com.orderapp.ordering.dto.StationStatsResponse;
import com.orderapp.ordering.dto.StationSummaryResponse;
import com.orderapp.ordering.dto.StationUpdateRequest;
import com.orderapp.ordering.entity.StationStatus;
import com.orderapp.ordering.entity.StationType;
import com.orderapp.ordering.multitenant.TenantContext;
import com.orderapp.ordering.service.StationQrCodeService;
import com.orderapp.ordering.service.StationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff/stations")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
public class StaffStationController {
    private final StationService stationService;
    private final StationQrCodeService stationQrCodeService;

    @GetMapping
    public List<StationSummaryResponse> searchStations(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "areaId", required = false) Long areaId,
            @RequestParam(name = "type", required = false) StationType type,
            @RequestParam(name = "status", required = false) StationStatus status,
            @RequestParam(name = "active", required = false) Boolean active
    ) {
        return stationService.searchStations(TenantContext.getTenantId(), new StationFilterRequest(name, areaId, type, status, active));
    }

    @GetMapping("/stats")
    public StationStatsResponse getStats() {
        return stationService.getStats(TenantContext.getTenantId());
    }

    @GetMapping("/{stationId}")
    public StationResponse getStation(@PathVariable Long stationId) {
        return stationService.getStationById(TenantContext.getTenantId(), stationId);
    }

    @PostMapping
    public ResponseEntity<StationResponse> createStation(@Valid @RequestBody StationCreateRequest request) {
        return ResponseEntity.status(201).body(stationService.createStation(TenantContext.getTenantId(), request));
    }

    @PutMapping("/{stationId}")
    public StationResponse updateStation(@PathVariable Long stationId, @Valid @RequestBody StationUpdateRequest request) {
        return stationService.updateStation(TenantContext.getTenantId(), stationId, request);
    }

    @DeleteMapping("/{stationId}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long stationId) {
        stationService.deleteStation(TenantContext.getTenantId(), stationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{stationId}/qr")
    public StationQrResponse getStationQr(@PathVariable Long stationId) {
        return stationQrCodeService.getActiveQrForStation(TenantContext.getTenantId(), stationId);
    }

    @PostMapping("/{stationId}/qr")
    public StationQrResponse generateStationQr(@PathVariable Long stationId, @RequestParam(name = "regenerate", defaultValue = "false") boolean regenerate) {
        return stationQrCodeService.generateQrForStation(TenantContext.getTenantId(), stationId, regenerate);
    }

    @PostMapping("/{stationId}/qr/regenerate")
    public StationQrResponse regenerateStationQr(@PathVariable Long stationId) {
        return stationQrCodeService.regenerateQrForStation(TenantContext.getTenantId(), stationId);
    }

    @GetMapping("/{stationId}/qr/download")
    public ResponseEntity<byte[]> downloadStationQr(@PathVariable Long stationId) {
        byte[] image = stationQrCodeService.downloadQrPng(TenantContext.getTenantId(), stationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=station-" + stationId + "-qr.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @GetMapping("/qr/download-all")
    public ResponseEntity<byte[]> downloadAllQrs() {
        byte[] zip = stationQrCodeService.downloadAllQrs(TenantContext.getTenantId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stations-qr.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }
}
