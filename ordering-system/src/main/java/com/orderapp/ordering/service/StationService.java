package com.orderapp.ordering.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.orderapp.ordering.dto.StationCreateRequest;
import com.orderapp.ordering.dto.StationFilterRequest;
import com.orderapp.ordering.dto.StationResponse;
import com.orderapp.ordering.dto.StationStatsResponse;
import com.orderapp.ordering.dto.StationSummaryResponse;
import com.orderapp.ordering.dto.StationUpdateRequest;
import com.orderapp.ordering.entity.AreaEntity;
import com.orderapp.ordering.entity.StationEntity;
import com.orderapp.ordering.entity.StationQrCodeEntity;
import com.orderapp.ordering.entity.StationStatus;
import com.orderapp.ordering.entity.StationType;
import com.orderapp.ordering.exception.BusinessException;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.repository.StationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StationService {
	private final StationRepository stationRepository;
	private final AreaService areaService;
	private final StationQrCodeService stationQrCodeService;
	private final DemoGuard demoGuard;

	public List<StationSummaryResponse> searchStations(Long tenantId, StationFilterRequest filter) {
		String name = filter != null ? filter.name() : null;
		String namePattern = name == null || name.isBlank() ? "" : "%" + name.trim().toLowerCase() + "%";
		Long areaId = filter != null ? filter.areaId() : null;
		StationType type = filter != null ? filter.type() : null;
		StationStatus status = filter != null ? filter.status() : null;
		Boolean active = filter != null ? filter.active() : null;

		return stationRepository.searchStations(
						tenantId,
						namePattern,
						areaId,
						type != null ? type.toDatabaseValue() : null,
						status != null ? status.name() : null,
						active)
				.stream()
				.map(station -> toSummaryResponse(tenantId, station))
				.toList();
	}

	public StationResponse getStationById(Long tenantId, Long stationId) {
		StationEntity station = loadStation(tenantId, stationId);
		return toResponse(station, loadActiveQrCodeOrNull(tenantId, stationId));
	}

	public StationResponse createStation(Long tenantId, StationCreateRequest request) {
		demoGuard.checkNotDemo(tenantId);
		validateNameUniqueness(tenantId, request.name(), null);

		AreaEntity area = areaService.requireTenantArea(tenantId, request.areaId());
		OffsetDateTime now = OffsetDateTime.now();

		StationEntity station = StationEntity.builder()
				.tenantId(tenantId)
				.area(area)
				.type((request.type() != null ? request.type() : StationType.SERVICE_POINT).toDatabaseValue())
				.name(request.name().trim())
				.capacity(request.capacity())
				.status(request.active() == null || request.active() ? "ACTIVE" : "DISABLED")
				.operationalStatus((request.status() != null ? request.status() : StationStatus.AVAILABLE).name())
				.metadataJson("{}")
				.createdAt(now)
				.updatedAt(now)
				.build();

		StationEntity saved = stationRepository.save(station);

		if (Boolean.TRUE.equals(request.generateQrAutomatically())) {
			stationQrCodeService.generateQrForStation(tenantId, saved.getId(), false);
		}

		return toResponse(saved, loadActiveQrCodeOrNull(tenantId, saved.getId()));
	}

	public StationResponse updateStation(Long tenantId, Long stationId, StationUpdateRequest request) {
		demoGuard.checkNotDemo(tenantId);
		StationEntity station = loadStation(tenantId, stationId);
		validateNameUniqueness(tenantId, request.name(), stationId);

		station.setName(request.name().trim());
		if (request.type() != null) {
			station.setType(request.type().toDatabaseValue());
		}
		if (request.areaId() != null) {
			station.setArea(areaService.requireTenantArea(tenantId, request.areaId()));
		}
		if (request.capacity() != null) {
			station.setCapacity(request.capacity());
		}
		if (request.status() != null) {
			station.setOperationalStatus(request.status().name());
		}
		if (station.getMetadataJson() == null || station.getMetadataJson().isBlank()) {
			station.setMetadataJson("{}");
		}
		if (request.active() != null) {
			station.setActive(request.active());
		}

		station.setUpdatedAt(OffsetDateTime.now());
		StationEntity saved = stationRepository.save(station);
		return toResponse(saved, loadActiveQrCodeOrNull(tenantId, saved.getId()));
	}

	public void deleteStation(Long tenantId, Long stationId) {
		demoGuard.checkNotDemo(tenantId);
		StationEntity station = loadStation(tenantId, stationId);
		long totalOrders = stationRepository.countOrdersByStationId(tenantId, stationId);
		if (totalOrders > 0) {
			throw new BusinessException("Impossibile eliminare la postazione: esistono ordini collegati. Disattivala invece se vuoi toglierla dagli ordini clienti.");
		}

		try {
			stationRepository.delete(station);
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("Impossibile eliminare la postazione: esistono dati collegati.");
		}
	}

	public StationStatsResponse getStats(Long tenantId) {
		long total = stationRepository.countByTenantId(tenantId);
		long available = stationRepository.countAvailableByTenantId(tenantId);
		long occupied = stationRepository.countOccupiedByTenantId(tenantId);
		long closed = stationRepository.countClosedByTenantId(tenantId);
		long activeOrders = stationRepository.countWithActiveOrdersByTenantId(tenantId);

		long reserved = stationRepository.searchStations(tenantId, "", null, null, StationStatus.RESERVED.name(), true).size();
		long orderingDisabled = stationRepository.searchStations(tenantId, "", null, null, StationStatus.ORDERING_DISABLED.name(), true).size();

		return new StationStatsResponse(total, available, occupied, reserved, orderingDisabled, closed, activeOrders);
	}

	private StationEntity loadStation(Long tenantId, Long stationId) {
		return stationRepository.findByTenantIdAndId(tenantId, stationId)
				.orElseThrow(() -> new ResourceNotFoundException("Postazione non trovata"));
	}

	private void validateNameUniqueness(Long tenantId, String name, Long stationId) {
		String normalized = name == null ? null : name.trim();
		if (normalized == null || normalized.isBlank()) {
			throw new BusinessException("Nome postazione obbligatorio");
		}

		boolean duplicate = stationId == null
				? stationRepository.existsByTenantIdAndNameIgnoreCase(tenantId, normalized)
				: stationRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, normalized, stationId);

		if (duplicate) {
			throw new BusinessException("Postazione già esistente");
		}
	}

	private StationQrCodeEntity loadActiveQrCodeOrNull(Long tenantId, Long stationId) {
		try {
			return stationQrCodeService.loadActiveQrEntity(tenantId, stationId);
		} catch (ResourceNotFoundException ex) {
			return null;
		}
	}

	private StationResponse toResponse(StationEntity station, StationQrCodeEntity qrCode) {
		return new StationResponse(
				station.getId(),
				station.getTenantId(),
				station.getName(),
				StationType.fromDatabaseValue(station.getType()),
				station.getArea() != null ? station.getArea().getId() : null,
				station.getArea() != null ? station.getArea().getName() : null,
				station.getCapacity(),
				StationStatus.fromDatabaseValue(station.getOperationalStatus()),
				station.isActive(),
				qrCode != null ? qrCode.getCode() : null,
				qrCode != null ? qrCode.getQrValue() : null,
				qrCode != null && qrCode.isActive(),
				station.getCreatedAt(),
				station.getUpdatedAt()
		);
	}

	private StationSummaryResponse toSummaryResponse(Long tenantId, StationEntity station) {
		StationQrCodeEntity qrCode = loadActiveQrCodeOrNull(tenantId, station.getId());
		long activeOrders = stationRepository.countActiveOrdersByStationId(tenantId, station.getId());

		return new StationSummaryResponse(
				station.getId(),
				station.getName(),
				StationType.fromDatabaseValue(station.getType()),
				station.getArea() != null ? station.getArea().getId() : null,
				station.getArea() != null ? station.getArea().getName() : null,
				station.getCapacity(),
				StationStatus.fromDatabaseValue(station.getOperationalStatus()),
				station.isActive(),
				qrCode != null ? qrCode.getCode() : null,
				activeOrders
		);
	}
}