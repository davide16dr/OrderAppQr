# Exception Handling & Validation Analysis - Spring Boot Backend

## Overview
The Spring Boot backend uses two main exception handlers that return HTTP 400 (Bad Request) responses. Below is the complete mapping of where these errors originate.

---

## 1. Exception Handlers

### A. ApiExceptionHandler
**File:** [ordering-system/src/main/java/com/orderapp/ordering/config/ApiExceptionHandler.java](ordering-system/src/main/java/com/orderapp/ordering/config/ApiExceptionHandler.java)

Handles tenant resolution errors:
```java
@ExceptionHandler(TenantNotResolvedException.class)
public ResponseEntity<?> handleTenantNotResolved(TenantNotResolvedException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
        "timestamp", OffsetDateTime.now().toString(),
        "status", 400,
        "error", "TENANT_NOT_RESOLVED",
        "message", ex.getMessage(),
        "path", request.getRequestURI()
    ));
}
```

### B. GlobalExceptionHandler
**File:** [ordering-system/src/main/java/com/orderapp/ordering/config/GlobalExceptionHandler.java](ordering-system/src/main/java/com/orderapp/ordering/config/GlobalExceptionHandler.java)

Maps multiple exceptions to HTTP 400:

#### 1. MethodArgumentNotValidException (Validation Errors)
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidationExceptions(...)
// Returns: 400 with field-specific validation errors
```

#### 2. IllegalArgumentException
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(...)
// Returns: 400 with the exception message
```

#### 3. BusinessException
```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<Map<String, Object>> handleBusinessException(...)
// Returns: 400 with business error message
```

#### 4. DataIntegrityViolationException
```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(...)
// Returns: 400 with message "Impossibile eliminare la postazione: esistono dati collegati."
```

---

## 2. TenantNotResolvedException

**File:** [ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantNotResolvedException.java](ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantNotResolvedException.java)

Thrown in [TenantResolutionFilter.java](ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantResolutionFilter.java):

```java
// Line 92: Invalid xTenantId query parameter
throw new TenantNotResolvedException("Invalid xTenantId query parameter: " + xTenantIdParam);

// Line 96: Missing subdomain in Host header
throw new TenantNotResolvedException("Missing tenant subdomain in Host header");

// Line 102: Invalid X-Tenant-Id header value
throw new TenantNotResolvedException("Invalid X-Tenant-Id header value: " + tenantIdHeader);
```

**Response Format (HTTP 400):**
```json
{
  "timestamp": "2026-05-28T10:30:00Z",
  "status": 400,
  "error": "TENANT_NOT_RESOLVED",
  "message": "Invalid xTenantId query parameter: abc",
  "path": "/api/dashboard/metrics"
}
```

---

## 3. BusinessException

**File:** [ordering-system/src/main/java/com/orderapp/ordering/exception/BusinessException.java](ordering-system/src/main/java/com/orderapp/ordering/exception/BusinessException.java)

Thrown from multiple services:

### CategoryService
- `throw new BusinessException("Il nome della categoria è obbligatorio");` (Line 76)
- `throw new BusinessException("Una categoria con questo nome esiste già");` (Lines 83, 116)
- `throw new BusinessException("Impossibile eliminare la categoria");` (Line 147)

### StationService
- `throw new BusinessException("Impossibile eliminare la postazione: esistono ordini collegati. Disattivala invece se vuoi toglierla dagli ordini clienti.");` (Line 119)
- `throw new BusinessException("Impossibile eliminare la postazione: esistono dati collegati.");` (Line 125)
- `throw new BusinessException("Nome postazione obbligatorio");` (Line 150)
- `throw new BusinessException("Postazione già esistente");` (Line 158)

### StationQrCodeService
- `throw new BusinessException("Impossibile creare l'archivio ZIP dei QR");` (Line 201)
- `throw new BusinessException("Impossibile generare il QR code");` (Line 288)
- `throw new BusinessException("Impossibile generare il QR code con informazioni");` (Line 356)

---

## 4. IllegalArgumentException

**Primary Source:** [PublicOrderService.java](ordering-system/src/main/java/com/orderapp/ordering/service/PublicOrderService.java)

Validation errors that return 400:

```java
// Line 100: Ordering disabled
throw new IllegalArgumentException("Le ordinazioni non sono disponibili in questo momento");

// Line 104: No products selected
throw new IllegalArgumentException("Nessun prodotto selezionato");

// Line 120: Missing tenantProductId
throw new IllegalArgumentException("tenantProductId mancante alla riga " + i);

// Line 123: Invalid quantity
throw new IllegalArgumentException("quantity non valida alla riga " + i);

// Line 232: Product not available
throw new IllegalArgumentException("Prodotto non disponibile: " + requestLine.getTenantProductId());

// Line 245: Invalid option
throw new IllegalArgumentException("Opzione non valida: " + optionId);

// Line 248: Option not associated with product
throw new IllegalArgumentException("Opzione non associata al prodotto: " + optionId);
```

**Also from CategoryService:**
```java
throw new IllegalArgumentException("tenantId must be positive");
throw new IllegalArgumentException("tenantId and categoryId must be positive");
```

**Also from DashboardService:**
```java
throw new IllegalArgumentException("Invalid settings payload", ex);
throw new IllegalArgumentException("Invalid time format for " + field + ": " + value);
throw new IllegalArgumentException("Product name is required");
```

---

## 5. DTO Validation Constraints

### BusinessSignupRequest
**File:** [ordering-system/src/main/java/com/orderapp/ordering/dto/BusinessSignupRequest.java](ordering-system/src/main/java/com/orderapp/ordering/dto/BusinessSignupRequest.java)

All fields validated with `@NotBlank`, `@Size`, `@Email`, `@Pattern`:
- `tenantName` - @NotBlank, @Size(2-255)
- `legalName` - @NotBlank, @Size(2-255)
- `businessType` - @NotBlank, @Pattern(LIDO|BAR|RESTAURANT|NIGHTCLUB|OTHER)
- `requestedSlug` - @NotBlank, @Pattern(^[a-z0-9]+(?:-[a-z0-9]+)*$)
- `contactEmail`, `businessEmail` - @Email validation
- Many more with size constraints...

### CreateTenantCategoryRequestDto
**File:** [ordering-system/src/main/java/com/orderapp/ordering/dto/CreateTenantCategoryRequestDto.java](ordering-system/src/main/java/com/orderapp/ordering/dto/CreateTenantCategoryRequestDto.java)

```java
@NotBlank(message = "Nome categoria obbligatorio")
@Size(min = 2, max = 100, message = "Nome deve essere tra 2 e 100 caratteri")
String name,

@Size(max = 500, message = "Descrizione massimo 500 caratteri")
String description,

@Min(value = 0, message = "Display order deve essere >= 0")
Integer displayOrder
```

### CreateTenantProductRequestDto
**File:** [ordering-system/src/main/java/com/orderapp/ordering/dto/CreateTenantProductRequestDto.java](ordering-system/src/main/java/com/orderapp/ordering/dto/CreateTenantProductRequestDto.java)

No validation annotations on class fields (not required)

### AreaCreateRequest
Validation: `@NotBlank(message = "Nome area obbligatorio")`

### UpdateTenantSettingsRequestDto
**File:** [ordering-system/src/main/java/com/orderapp/ordering/dto/UpdateTenantSettingsRequestDto.java](ordering-system/src/main/java/com/orderapp/ordering/dto/UpdateTenantSettingsRequestDto.java)

No validation annotations (all fields optional):
```java
public record UpdateTenantSettingsRequestDto(
    String openingTime,
    String closingTime,
    Boolean orderingPaused,
    String ordersViewStartTime,
    String ordersViewEndTime
)
```

### UpdateTenantCategoryRequestDto
**File:** [ordering-system/src/main/java/com/orderapp/ordering/dto/UpdateTenantCategoryRequestDto.java](ordering-system/src/main/java/com/orderapp/ordering/dto/UpdateTenantCategoryRequestDto.java)

No validation annotations

### UpdateTenantProductRequestDto
**File:** [ordering-system/src/main/java/com/orderapp/ordering/dto/UpdateTenantProductRequestDto.java](ordering-system/src/main/java/com/orderapp/ordering/dto/UpdateTenantProductRequestDto.java)

No validation annotations

---

## 6. Controllers Triggering Validation

### DashboardController
**File:** [ordering-system/src/main/java/com/orderapp/ordering/controller/DashboardController.java](ordering-system/src/main/java/com/orderapp/ordering/controller/DashboardController.java)

- `POST /api/dashboard/products` - CreateTenantProductRequestDto
- `PATCH /api/dashboard/products/{productId}` - UpdateTenantProductRequestDto
- `DELETE /api/dashboard/products/{productId}` - calls dashboardService.disableTenantProduct()
- `POST /api/dashboard/categories` - CreateTenantCategoryRequestDto
- `PATCH /api/dashboard/categories/{categoryId}` - UpdateTenantCategoryRequestDto
- `DELETE /api/dashboard/categories/{categoryId}` - calls categoryService.deleteTenantCategory()
- `PATCH /api/dashboard/settings` - UpdateTenantSettingsRequestDto

### PublicCustomerController
**File:** [ordering-system/src/main/java/com/orderapp/ordering/controller/PublicCustomerController.java](ordering-system/src/main/java/com/orderapp/ordering/controller/PublicCustomerController.java)

- `POST /api/public/orders` - Creates orders with extensive validation in PublicOrderService

---

## 7. Flow Summary for 400 Errors

```
Client Request
    ↓
TenantResolutionFilter (checks Host/X-Tenant-Id/tenant param)
    ├→ TenantNotResolvedException (HTTP 400 - ApiExceptionHandler)
    ↓
DashboardController / Other Controllers
    ├→ MethodArgumentNotValidException (HTTP 400 - GlobalExceptionHandler)
    │  └─ Validation failures on @Valid DTOs
    ├→ Service Methods
    │  ├→ BusinessException (HTTP 400 - GlobalExceptionHandler)
    │  ├→ IllegalArgumentException (HTTP 400 - GlobalExceptionHandler)
    │  └→ DataIntegrityViolationException (HTTP 400 - GlobalExceptionHandler)
    ↓
Response with errorDetails
```

---

## 8. Key Files Summary

| Component | File Path | Purpose |
|-----------|-----------|---------|
| Exception Handler | `config/ApiExceptionHandler.java` | Handles TenantNotResolvedException → 400 |
| Exception Handler | `config/GlobalExceptionHandler.java` | Handles validation, business, and illegal argument errors → 400 |
| Tenant Resolution | `multitenant/TenantResolutionFilter.java` | Validates tenant context; throws TenantNotResolvedException |
| Exception Class | `exception/BusinessException.java` | Custom exception for business logic errors |
| Exception Class | `multitenant/TenantNotResolvedException.java` | Custom exception for tenant resolution failures |
| Dashboard API | `controller/DashboardController.java` | Main endpoint for product/category/order management |
| Business Logic | `service/DashboardService.java` | Handles product/category/order operations |
| Business Logic | `service/CategoryService.java` | Category CRUD operations with validation |
| Order Processing | `service/PublicOrderService.java` | Order creation with extensive validation |
| Data Access | `repository/DashboardRepository.java` | JDBC queries for dashboard data |

---

## 9. Common 400 Error Scenarios

1. **Missing Tenant Context**
   - Missing `Host` header subdomain
   - Missing `X-Tenant-Id` header
   - Invalid `tenant` query parameter
   → Error: "TENANT_NOT_RESOLVED"

2. **Invalid Request Data**
   - Null/blank required fields (BusinessSignupRequest validation)
   - Email format invalid
   - Slug format invalid (must be lowercase with hyphens)
   → Error: Field-specific validation messages

3. **Business Logic Validation**
   - Category already exists
   - No products in order
   - Product not available for ordering
   - Invalid product options
   → Error: Specific business error message in Italian

4. **Category/Station Operations**
   - Deleting category with linked products
   - Deleting station with linked orders
   → Error: "Impossibile eliminare..."

5. **Settings Validation**
   - Invalid time format in opening/closing times
   - Invalid settings JSON payload
   → Error: "Invalid settings payload" or "Invalid time format"
