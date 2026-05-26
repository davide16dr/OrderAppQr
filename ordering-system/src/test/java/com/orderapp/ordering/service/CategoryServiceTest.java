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

import com.orderapp.ordering.dto.CreateTenantCategoryRequestDto;
import com.orderapp.ordering.dto.TenantCategoryDto;
import com.orderapp.ordering.dto.UpdateTenantCategoryRequestDto;
import com.orderapp.ordering.exception.BusinessException;
import com.orderapp.ordering.exception.ResourceNotFoundException;
import com.orderapp.ordering.repository.CategoryRepository;

@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    private CategoryService service;

    @Mock
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CategoryService(categoryRepository);
    }

    @Test
    @DisplayName("Should get tenant categories")
    void testGetTenantCategories() {
        // Arrange
        TenantCategoryDto category1 = new TenantCategoryDto(1L, "Bevande", "Bevande varie", 0, "ACTIVE");
        when(categoryRepository.getTenantCategories(1L))
                .thenReturn(List.of(category1));

        // Act
        List<TenantCategoryDto> result = service.getTenantCategories(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bevande", result.get(0).name());
        verify(categoryRepository, times(1)).getTenantCategories(1L);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid tenantId")
    void testGetTenantCategoriesInvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.getTenantCategories(-1L));
        assertThrows(IllegalArgumentException.class, () -> service.getTenantCategories(null));
    }

    @Test
    @DisplayName("Should get tenant category by id")
    void testGetTenantCategoryById() {
        // Arrange
        TenantCategoryDto category = new TenantCategoryDto(1L, "Bevande", "Bevande varie", 0, "ACTIVE");
        when(categoryRepository.getTenantCategoryById(1L, 1L))
                .thenReturn(Optional.of(category));

        // Act
        TenantCategoryDto result = service.getTenantCategoryById(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals("Bevande", result.name());
        verify(categoryRepository, times(1)).getTenantCategoryById(1L, 1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void testGetTenantCategoryByIdNotFound() {
        // Arrange
        when(categoryRepository.getTenantCategoryById(1L, 999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getTenantCategoryById(1L, 999L));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid ids")
    void testGetTenantCategoryByIdInvalidIds() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.getTenantCategoryById(-1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.getTenantCategoryById(1L, -1L));
        assertThrows(IllegalArgumentException.class, () -> service.getTenantCategoryById(null, 1L));
    }

    @Test
    @DisplayName("Should create category successfully")
    void testCreateTenantCategory() {
        // Arrange
        CreateTenantCategoryRequestDto request = new CreateTenantCategoryRequestDto("Bevande", null, 0);
        TenantCategoryDto category = new TenantCategoryDto(1L, "Bevande", null, 0, "ACTIVE");

        when(categoryRepository.categoryNameExists(1L, "Bevande")).thenReturn(false);
        when(categoryRepository.createTenantCategory(1L, "Bevande", null, 0))
                .thenReturn(category);

        // Act
        TenantCategoryDto result = service.createTenantCategory(1L, request);

        // Assert
        assertNotNull(result);
        assertEquals("Bevande", result.name());
        verify(categoryRepository, times(1)).createTenantCategory(1L, "Bevande", null, 0);
    }

    @Test
    @DisplayName("Should throw BusinessException for empty category name")
    void testCreateTenantCategoryEmptyName() {
        // Arrange
        CreateTenantCategoryRequestDto request = new CreateTenantCategoryRequestDto("", null, 0);

        // Act & Assert
        assertThrows(BusinessException.class, () -> service.createTenantCategory(1L, request));
    }

    @Test
    @DisplayName("Should throw BusinessException for duplicate category name")
    void testCreateTenantCategoryDuplicate() {
        // Arrange
        CreateTenantCategoryRequestDto request = new CreateTenantCategoryRequestDto("Bevande", null, 0);
        when(categoryRepository.categoryNameExists(1L, "Bevande")).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> service.createTenantCategory(1L, request));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid tenantId on create")
    void testCreateTenantCategoryInvalidId() {
        // Arrange
        CreateTenantCategoryRequestDto request = new CreateTenantCategoryRequestDto("Bevande", null, 0);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.createTenantCategory(-1L, request));
        assertThrows(IllegalArgumentException.class, () -> service.createTenantCategory(null, request));
    }

    @Test
    @DisplayName("Should update category successfully")
    void testUpdateTenantCategory() {
        // Arrange
        UpdateTenantCategoryRequestDto request = new UpdateTenantCategoryRequestDto("Bevande Updated", "New description", 1, "ACTIVE");
        TenantCategoryDto existingCategory = new TenantCategoryDto(1L, "Bevande", "Bevande varie", 0, "ACTIVE");
        TenantCategoryDto updatedCategory = new TenantCategoryDto(1L, "Bevande Updated", "New description", 1, "ACTIVE");

        when(categoryRepository.getTenantCategoryById(1L, 1L))
                .thenReturn(Optional.of(existingCategory));
        when(categoryRepository.categoryNameExistsExcluding(1L, "Bevande Updated", 1L))
                .thenReturn(false);
        when(categoryRepository.updateTenantCategory(1L, 1L, "Bevande Updated", "New description", 1, "ACTIVE"))
                .thenReturn(updatedCategory);

        // Act
        TenantCategoryDto result = service.updateTenantCategory(1L, 1L, request);

        // Assert
        assertNotNull(result);
        assertEquals("Bevande Updated", result.name());
        verify(categoryRepository, times(1)).updateTenantCategory(1L, 1L, "Bevande Updated", "New description", 1, "ACTIVE");
    }

    @Test
    @DisplayName("Should throw BusinessException for duplicate name on update")
    void testUpdateTenantCategoryDuplicateName() {
        // Arrange
        UpdateTenantCategoryRequestDto request = new UpdateTenantCategoryRequestDto("Existing Name", null, null, null);
        TenantCategoryDto existingCategory = new TenantCategoryDto(1L, "Bevande", "Bevande varie", 0, "ACTIVE");

        when(categoryRepository.getTenantCategoryById(1L, 1L))
                .thenReturn(Optional.of(existingCategory));
        when(categoryRepository.categoryNameExistsExcluding(1L, "Existing Name", 1L))
                .thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> service.updateTenantCategory(1L, 1L, request));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void testDeleteTenantCategory() {
        // Arrange
        TenantCategoryDto category = new TenantCategoryDto(1L, "Bevande", null, 0, "ACTIVE");
        when(categoryRepository.getTenantCategoryById(1L, 1L))
                .thenReturn(Optional.of(category));
        when(categoryRepository.deleteTenantCategory(1L, 1L)).thenReturn(true);

        // Act
        service.deleteTenantCategory(1L, 1L);

        // Assert
        verify(categoryRepository, times(1)).deleteTenantCategory(1L, 1L);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid ids on delete")
    void testDeleteTenantCategoryInvalidIds() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.deleteTenantCategory(-1L, 1L));
        assertThrows(IllegalArgumentException.class, () -> service.deleteTenantCategory(1L, -1L));
    }
}
