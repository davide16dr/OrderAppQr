# Tenant Context Resolution Analysis - Spring Boot Backend

## Overview
The application uses a **multi-tenant architecture** where tenant context is resolved on incoming requests through a filter-based approach. The tenant ID is extracted, validated, and stored in thread-local storage for use throughout the request lifecycle.

---

## 1. TenantContext - Thread-Local Storage

**File:** [ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantContext.java](ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantContext.java)

```java
public final class TenantContext {
	private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
	private static final ThreadLocal<String> TENANT_SLUG = new ThreadLocal<>();

	private TenantContext() {
	}

	public static void setTenant(Long tenantId, String tenantSlug) {
		TENANT_ID.set(tenantId);
		TENANT_SLUG.set(tenantSlug);
	}

	public static Long getTenantId() {
		return TENANT_ID.get();
	}

	public static String getTenantSlug() {
		return TENANT_SLUG.get();
	}

	public static void clear() {
		TENANT_ID.remove();
		TENANT_SLUG.remove();
	}
}
```

**Purpose:** Stores the tenant ID and slug in thread-local variables so they can be accessed from anywhere in the request context without being passed as parameters.

---

## 2. TenantResolutionFilter - Request Interceptor

**File:** [ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantResolutionFilter.java](ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantResolutionFilter.java)

This is the **key filter** that resolves tenant ID from incoming requests. It runs **before** the controller layer.

### Resolution Strategy (in order of priority):

1. **Subdomain from Host header** (e.g., `tenant-slug.domain.com`)
   ```
   if subdomain != null && !subdomain.isBlank() {
       tenant = tenantResolverService.resolveBySubdomainOrThrow(subdomain);
   }
   ```

2. **Query parameter `tenant`** (for SockJS WebSocket connections)
   ```
   String tenantParam = request.getParameter("tenant");
   if (tenantParam != null && !tenantParam.isBlank()) {
       tenant = tenantResolverService.resolveBySubdomainOrThrow(tenantParam);
   }
   ```

3. **Query parameter `xTenantId`** (alternative numeric ID)
   ```
   String xTenantIdParam = request.getParameter("xTenantId");
   if (xTenantIdParam != null && !xTenantIdParam.isBlank()) {
       Long tenantId = Long.parseLong(xTenantIdParam);
       tenant = tenantResolverService.resolveByIdOrThrow(tenantId);
   }
   ```

4. **`X-Tenant-Id` header** (default fallback)
   ```
   String tenantIdHeader = request.getHeader("X-Tenant-Id");
   if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
       throw new TenantNotResolvedException("Missing tenant subdomain in Host header");
   }
   Long tenantId = Long.parseLong(tenantIdHeader);
   tenant = tenantResolverService.resolveByIdOrThrow(tenantId);
   ```

### Exclusions:

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    // Skip public endpoints
    return uri != null && uri.startsWith("/api/public/");
}

// Inside doFilterInternal:
if (requestUri != null && (requestUri.startsWith("/api/admin") || requestUri.startsWith("/admin"))) {
    filterChain.doFilter(request, response);
    return;
}

// Skip for SUPER_ADMIN users
boolean isSuperAdmin = authentication.getAuthorities().stream()
        .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
if (isSuperAdmin) {
    filterChain.doFilter(request, response);
    return;
}
```

### Error Handling:

```java
catch (TenantNotResolvedException ex) {
    response.setStatus(HttpStatus.BAD_REQUEST.value());  // 400
    response.setContentType("application/json");
    response.getWriter().write(objectMapper.writeValueAsString(Map.of(
        "timestamp", OffsetDateTime.now().toString(),
        "status", 400,
        "error", "TENANT_NOT_RESOLVED",
        "message", ex.getMessage(),
        "path", request.getRequestURI()
    )));
}
```

**Returns 400 Bad Request when:**
- `X-Tenant-Id` header is missing AND no subdomain/parameter is provided
- `X-Tenant-Id` value cannot be parsed as a Long
- Subdomain/tenant slug is invalid or not found
- Tenant exists but is not ACTIVE

---

## 3. TenantResolverService - Tenant Lookup

**File:** [ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantResolverService.java](ordering-system/src/main/java/com/orderapp/ordering/multitenant/TenantResolverService.java)

```java
@Service
@RequiredArgsConstructor
public class TenantResolverService {
	private final TenantRepository tenantRepository;

	public Tenant resolveBySubdomainOrThrow(String subdomain) {
		return tenantRepository.findBySubdomainIgnoreCase(subdomain)
			.filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))  // CRITICAL: Must be ACTIVE
			.orElseThrow(() -> new TenantNotResolvedException(
				"Tenant not found or not active for subdomain: " + subdomain));
	}

	public Tenant resolveByIdOrThrow(Long tenantId) {
		return tenantRepository.findById(tenantId)
			.orElseThrow(() -> new TenantNotResolvedException(
				"Tenant not found for id: " + tenantId));
	}
}
```

**Key Detail:** When resolving by subdomain, the tenant must have status = "ACTIVE". When resolving by ID, there's no status check (potential inconsistency).

---

## 4. How Request Flows to /api/dashboard/metrics

### Request Flow Diagram:

```
HTTP Request to /api/dashboard/metrics
    ↓
TenantResolutionFilter.doFilterInternal()
    ↓
Extract tenantId from:
  - Host header subdomain
  - ?tenant query param
  - ?xTenantId query param  
  - X-Tenant-Id header
    ↓
Call TenantResolverService.resolveByIdOrThrow(tenantId) or resolveBySubdomainOrThrow(subdomain)
    ↓
Set TenantContext.setTenant(tenantId, tenantSlug)
    ↓
Continue to DashboardController.getDashboardMetrics()
    ↓
@GetMapping("/metrics")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
    Long tenantId = TenantContext.getTenantId();  // ← Retrieved from thread-local
    DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(tenantId);
    return ResponseEntity.ok(metrics);
}
    ↓
Filter.doFilterInternal() finally block
    ↓
TenantContext.clear()  // Clean up thread-local
```

---

## 5. DashboardController - How Endpoint Validates Tenant

**File:** [ordering-system/src/main/java/com/orderapp/ordering/controller/DashboardController.java](ordering-system/src/main/java/com/orderapp/ordering/controller/DashboardController.java)

```java
@GetMapping("/metrics")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'BAR', 'KITCHEN')")
public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
    Long tenantId = TenantContext.getTenantId();
    log.info("[dashboard-controller] getDashboardMetrics start tenantId={} principal={}",
        tenantId,
        SecurityContextHolder.getContext().getAuthentication() != null
            ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
            : null);

    DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(tenantId);

    log.info("[dashboard-controller] getDashboardMetrics done tenantId={} totalOrdersToday={} topProducts={}",
        tenantId,
        metrics.getTotalOrdersToday(),
        metrics.getTopProducts() != null ? metrics.getTopProducts().size() : 0);

    return ResponseEntity.ok(metrics);
}
```

**Validation:**
1. **Role check:** `@PreAuthorize` ensures user has one of: ADMIN, STAFF, MANAGER, BAR, KITCHEN
2. **Tenant extraction:** `TenantContext.getTenantId()` retrieves the tenant ID set by the filter
3. **No explicit null check:** If `tenantId` is null, it gets passed to the service layer
4. **Service layer validation:** The DashboardService queries metrics using this tenantId

---

## 6. JWT Authentication Filter - User Authentication

**File:** [ordering-system/src/main/java/com/orderapp/ordering/security/JwtAuthenticationFilter.java](ordering-system/src/main/java/com/orderapp/ordering/security/JwtAuthenticationFilter.java)

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);
            
            // Extract user ID and roles from JWT token
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String userId = jwtTokenProvider.getUserIdFromToken(token);
                List<GrantedAuthority> authorities = extractAuthoritiesFromToken(token);
                
                // Create authentication with user info and roles
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId, 
                        null, 
                        authorities
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("JWT authentication failed", ex);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**Note:** This filter handles **user authentication** (who you are), while `TenantResolutionFilter` handles **tenant context** (which tenant you belong to). They work independently.

---

## 7. Potential Issues Causing 400 Bad Request

### Issue 1: Missing X-Tenant-Id Header
**Symptom:** `400 Bad Request - "Missing tenant subdomain in Host header"`

**Root Cause:** 
- No subdomain in Host header
- No `?tenant` query parameter
- No `?xTenantId` query parameter  
- No `X-Tenant-Id` header

**Solution:** Frontend must send one of these:
```javascript
// Option A: X-Tenant-Id header
headers: {
    'X-Tenant-Id': '1',
    'Authorization': 'Bearer ' + token
}

// Option B: Subdomain in Host
// Request to: tenant-slug.localhost:3000/api/dashboard/metrics

// Option C: Query parameter
// GET /api/dashboard/metrics?xTenantId=1
```

### Issue 2: Invalid Tenant ID Format
**Symptom:** `400 Bad Request - "Invalid X-Tenant-Id header value: abc"`

**Root Cause:** X-Tenant-Id header or xTenantId parameter contains non-numeric value

**Fix:**
```javascript
// WRONG:
'X-Tenant-Id': 'tenant-slug'

// CORRECT:
'X-Tenant-Id': '1'  // Must be a numeric ID
```

### Issue 3: Tenant Not Found
**Symptom:** `400 Bad Request - "Tenant not found for id: 999"`

**Root Cause:** Tenant ID doesn't exist in database

**Debug:** Check the tenants table:
```sql
SELECT id, subdomain, slug, status FROM tenants;
```

### Issue 4: Tenant Not Active
**Symptom:** `400 Bad Request - "Tenant not found or not active for subdomain: ..."`

**Root Cause:** Tenant exists but status ≠ "ACTIVE"

**Debug:** Check tenant status:
```sql
SELECT id, subdomain, status FROM tenants WHERE subdomain = 'your-tenant';
```

### Issue 5: Missing Authorization Header
**Symptom:** `401 Unauthorized` (not 400, but related)

**Root Cause:** No JWT token or token invalid

**Fix:** Include valid JWT token:
```javascript
headers: {
    'Authorization': 'Bearer ' + jwtToken,
    'X-Tenant-Id': tenantId
}
```

---

## 8. Request Sequence Diagram

```
┌─────────┐                                          ┌──────────────────────────────┐
│ Browser │                                          │ Spring Boot Backend          │
└────┬────┘                                          └──────────────────────────────┘
     │
     │ GET /api/dashboard/metrics
     │ Headers: {
     │   Authorization: Bearer JWT_TOKEN,
     │   X-Tenant-Id: 1
     │ }
     ├────────────────────────────────────────────→ JwtAuthenticationFilter
     │                                              (extract user from JWT)
     │                                                      ↓
     │                                              TenantResolutionFilter
     │                                              (extract tenantId from header)
     │                                                      ↓
     │                                              TenantContext.setTenant(1, 'slug')
     │                                                      ↓
     │                                              DashboardController.getDashboardMetrics()
     │                                              (call TenantContext.getTenantId() → 1)
     │                                                      ↓
     │                                              DashboardService.getDashboardMetrics(1)
     │                                              (query database for tenant 1 data)
     │                                                      ↓
     │                                              DashboardMetricsDto { ... }
     │                                                      ↓
     │                                              TenantContext.clear()
     │ ← ────────────────────────────────────────  HTTP 200 OK + metrics JSON
     │
```

---

## 9. Key Configuration Points

### Filter Order (SecurityConfiguration):
The `TenantResolutionFilter` typically runs after `JwtAuthenticationFilter` because:
1. First: Extract user identity (JWT)
2. Second: Extract tenant context

### Important: Admin/Super-Admin Bypass
Admin endpoints and users with ROLE_SUPER_ADMIN skip tenant context resolution entirely. This allows admins to access cross-tenant operations.

### Thread-Local Cleanup
The filter includes a `finally` block to always call `TenantContext.clear()`, preventing tenant context leakage to other requests handled by the same thread (thread pool).

---

## 10. Debugging Checklist

If `/api/dashboard/metrics` returns 400 Bad Request:

- [ ] Check request headers: `Authorization`, `X-Tenant-Id`
- [ ] Verify JWT token is valid (not expired)
- [ ] Check if tenantId is numeric (e.g., `1`, not `"1"`)
- [ ] Verify tenant exists: `SELECT * FROM tenants WHERE id = YOUR_ID`
- [ ] Verify tenant status: Should be `ACTIVE`
- [ ] Check logs for: `[tenant-filter] Tenant resolution failed`
- [ ] If using subdomain: Verify Host header contains `tenant-slug.domain.com`
- [ ] If using query param: Verify `?xTenantId=1` is in URL
- [ ] Check that user has required role: ADMIN, STAFF, MANAGER, BAR, or KITCHEN

---

## Summary

**Tenant Context Resolution Flow:**
1. **JwtAuthenticationFilter** → Extracts user identity from JWT token
2. **TenantResolutionFilter** → Extracts tenant ID from header/param/subdomain
3. **TenantContext.setTenant()** → Stores in thread-local storage
4. **DashboardController** → Retrieves tenant ID via `TenantContext.getTenantId()`
5. **TenantContext.clear()** → Cleans up thread-local after response

**400 Bad Request = Tenant context could not be resolved** (missing header, invalid ID, tenant not found, or tenant not active)
