package com.orderapp.ordering.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.orderapp.ordering.entity.Tenant;
import com.orderapp.ordering.model.dto.TenantSummaryDto;
import com.orderapp.ordering.repository.TenantRepository;

import jakarta.persistence.EntityNotFoundException;

@DisplayName("AdminTenantService Unit Tests")
class AdminTenantServiceTest {

    private AdminTenantService service;

    @Mock
    private TenantRepository tenantRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AdminTenantService(tenantRepository);
    }

    @Test
    @DisplayName("Should get all tenants with pagination")
    void testGetAllTenants() {
        // Arrange
        Tenant tenant1 = new Tenant();
        tenant1.setId(1L);
        tenant1.setName("Test Tenant 1");
        tenant1.setSlug("test-tenant-1");
        tenant1.setEnabled(true);

        Page<Tenant> tenantPage = new PageImpl<>(List.of(tenant1));
        Pageable pageable = PageRequest.of(0, 20);
        
        when(tenantRepository.findAll(pageable)).thenReturn(tenantPage);

        // Act
        Page<TenantSummaryDto> result = service.getAllTenants(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Tenant 1", result.getContent().get(0).getName());
        verify(tenantRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("Should get tenant by id")
    void testGetTenantById() {
        // Arrange
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");
        tenant.setSlug("test-tenant");
        tenant.setEnabled(true);

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        // Act
        Tenant result = service.getTenantById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Tenant", result.getName());
        verify(tenantRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when tenant not found")
    void testGetTenantByIdNotFound() {
        // Arrange
        when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> service.getTenantById(999L));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid tenantId")
    void testGetTenantByIdInvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.getTenantById(-1L));
        assertThrows(IllegalArgumentException.class, () -> service.getTenantById(null));
    }

    @Test
    @DisplayName("Should update tenant status")
    void testUpdateTenantStatus() {
        // Arrange
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");
        tenant.setEnabled(true);

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        // Act
        service.updateTenantStatus(1L, false);

        // Assert
        assertFalse(tenant.isEnabled());
        verify(tenantRepository, times(1)).findById(1L);
        verify(tenantRepository, times(1)).save(tenant);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid tenantId on update")
    void testUpdateTenantStatusInvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.updateTenantStatus(-1L, true));
        assertThrows(IllegalArgumentException.class, () -> service.updateTenantStatus(null, true));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent tenant")
    void testUpdateTenantStatusNotFound() {
        // Arrange
        when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> service.updateTenantStatus(999L, false));
    }
}
