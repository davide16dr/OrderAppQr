package com.orderapp.ordering.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.orderapp.ordering.dto.AreaCreateRequest;
import com.orderapp.ordering.dto.AreaResponse;
import com.orderapp.ordering.dto.AreaUpdateRequest;
import com.orderapp.ordering.entity.AreaEntity;
import com.orderapp.ordering.exception.BusinessException;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.repository.AreaRepository;

@DisplayName("AreaService Unit Tests")
class AreaServiceTest {

    private AreaService service;

    @Mock
    private AreaRepository areaRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AreaService(areaRepository);
    }

    @Test
    @DisplayName("Should list areas by tenant")
    void testListAreasByTenant() {
        // Arrange
        AreaEntity area1 = AreaEntity.builder()
                .id(1L)
                .tenantId(1L)
                .name("Terrazza")
                .description("Area principale")
                .displayOrder(0)
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(areaRepository.findByTenantIdOrderByDisplayOrderAscNameAsc(1L))
                .thenReturn(List.of(area1));

        // Act
        List<AreaResponse> result = service.listAreasByTenant(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Terrazza", result.get(0).name());
        verify(areaRepository, times(1)).findByTenantIdOrderByDisplayOrderAscNameAsc(1L);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid tenantId")
    void testListAreasByTenantInvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.listAreasByTenant(-1L));
        assertThrows(IllegalArgumentException.class, () -> service.listAreasByTenant(null));
    }

    @Test
    @DisplayName("Should get area by id")
    void testGetAreaById() {
        // Arrange
        AreaEntity area = AreaEntity.builder()
                .id(1L)
                .tenantId(1L)
                .name("Terrazza")
                .description("Area principale")
                .displayOrder(0)
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(areaRepository.findByTenantIdAndId(1L, 1L)).thenReturn(Optional.of(area));

        // Act
        AreaResponse result = service.getAreaById(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Terrazza", result.name());
        verify(areaRepository, times(1)).findByTenantIdAndId(1L, 1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when area not found")
    void testGetAreaByIdNotFound() {
        // Arrange
        when(areaRepository.findByTenantIdAndId(1L, 999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getAreaById(1L, 999L));
    }

    @Test
    @DisplayName("Should create area successfully")
    void testCreateArea() {
        // Arrange
        AreaCreateRequest request = new AreaCreateRequest("Terrazza", "Area principale");
        AreaEntity area = AreaEntity.builder()
                .id(1L)
                .tenantId(1L)
                .name("Terrazza")
                .description("Area principale")
                .displayOrder(0)
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(areaRepository.existsByTenantIdAndNameIgnoreCase(1L, "Terrazza")).thenReturn(false);
        when(areaRepository.save(any(AreaEntity.class))).thenReturn(area);

        // Act
        AreaResponse result = service.createArea(1L, request);

        // Assert
        assertNotNull(result);
        assertEquals("Terrazza", result.name());
        verify(areaRepository, times(1)).save(any(AreaEntity.class));
    }

    @Test
    @DisplayName("Should throw BusinessException for duplicate area name")
    void testCreateAreaDuplicate() {
        // Arrange
        AreaCreateRequest request = new AreaCreateRequest("Terrazza", "Area principale");
        when(areaRepository.existsByTenantIdAndNameIgnoreCase(1L, "Terrazza")).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> service.createArea(1L, request));
    }

    @Test
    @DisplayName("Should throw BusinessException for empty area name")
    void testCreateAreaEmptyName() {
        // Arrange
        AreaCreateRequest request = new AreaCreateRequest("", "Area principale");

        // Act & Assert
        assertThrows(BusinessException.class, () -> service.createArea(1L, request));
    }

    @Test
    @DisplayName("Should update area successfully")
    void testUpdateArea() {
        // Arrange
        AreaUpdateRequest request = new AreaUpdateRequest("Terrazza Updated", "Updated description", true);
        AreaEntity area = AreaEntity.builder()
                .id(1L)
                .tenantId(1L)
                .name("Terrazza")
                .description("Area principale")
                .displayOrder(0)
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(areaRepository.findByTenantIdAndId(1L, 1L)).thenReturn(Optional.of(area));
        when(areaRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(1L, "Terrazza Updated", 1L)).thenReturn(false);
        when(areaRepository.save(area)).thenReturn(area);

        // Act
        AreaResponse result = service.updateArea(1L, 1L, request);

        // Assert
        assertNotNull(result);
        verify(areaRepository, times(1)).save(area);
    }

    @Test
    @DisplayName("Should delete area (soft delete)")
    void testDeleteArea() {
        // Arrange
        AreaEntity area = AreaEntity.builder()
                .id(1L)
                .tenantId(1L)
                .name("Terrazza")
                .description("Area principale")
                .displayOrder(0)
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(areaRepository.findByTenantIdAndId(1L, 1L)).thenReturn(Optional.of(area));
        when(areaRepository.save(area)).thenReturn(area);

        // Act
        service.deleteArea(1L, 1L);

        // Assert
        assertFalse(area.isActive());
        verify(areaRepository, times(1)).save(area);
    }
}
