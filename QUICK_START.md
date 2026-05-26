# 🚀 Quick Start Guide - OrderApp Backend

## Prerequisiti
- Java 17+
- Maven 3.9+
- PostgreSQL 14+ (production)
- Redis (production - opzionale in development)

---

## 1️⃣ Build & Compile

```bash
cd /Users/davideranavolo/Desktop/orderApp/OrderApp-1/ordering-system

# Clean & Compile
mvn clean compile

# Result: 
# [INFO] BUILD SUCCESS
# [INFO] Total time: 2.387 s

# Package (Create JAR)
mvn clean package -DskipTests

# Result:
# [INFO] BUILD SUCCESS
# JAR: target/ordering-system-0.0.1-SNAPSHOT.jar (82MB)
```

---

## 2️⃣ Run Unit Tests

```bash
# Run specific test classes
mvn test -Dtest=AdminTenantServiceTest,AreaServiceTest,CategoryServiceTest

# Result:
# [INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
# [INFO] BUILD SUCCESS

# Run all tests
mvn test

# With coverage report
mvn test jacoco:report
# Open: target/site/jacoco/index.html
```

---

## 3️⃣ Development Mode

```bash
# Start Spring Boot development server
mvn spring-boot:run

# Default profile: dev
# Cache: In-memory (fast, no Redis needed)
# Logging: DEBUG level
# Database: PostgreSQL on localhost:5432

# Output should contain:
# [INFO] Started OrderingSystemApplication in X.XXX seconds
# [INFO] Tomcat started on port(s): 8080
```

---

## 4️⃣ Access Endpoints

### Health (No Authentication)
```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/ready
curl http://localhost:8080/api/health/live
```

### Swagger UI Documentation
```
http://localhost:8080/swagger-ui.html
```

### API Endpoints (Require Bearer Token)

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "staff@example.com",
    "password": "password123"
  }'

# Response: 
# {
#   "accessToken": "eyJhbGci...",
#   "refreshToken": "...",
#   "expiresIn": 86400
# }
```

#### Admin Tenants (Paginated)
```bash
TOKEN="your_jwt_token_here"

# Get tenants
curl "http://localhost:8080/api/admin/tenants?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"

# Get single tenant
curl "http://localhost:8080/api/admin/tenants/1" \
  -H "Authorization: Bearer $TOKEN"

# Update tenant status
curl -X PATCH "http://localhost:8080/api/admin/tenants/1/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'
```

#### Async Categories
```bash
TOKEN="your_jwt_token_here"

# Get categories (async)
curl "http://localhost:8080/api/async/categories/1" \
  -H "Authorization: Bearer $TOKEN"

# Create category (async)
curl -X POST "http://localhost:8080/api/async/categories/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bevande",
    "description": "Bevande varie e cocktail",
    "displayOrder": 0
  }'

# Export categories (CSV)
curl "http://localhost:8080/api/async/categories/1/export" \
  -H "Authorization: Bearer $TOKEN" \
  -o categories.csv
```

---

## 5️⃣ Production Deployment

```bash
# Build production WAR/JAR
mvn clean package

# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET=your_very_secure_secret_key_here
export REDIS_HOST=redis.production.com
export REDIS_PASSWORD=redis_password
export REDIS_PORT=6379
export SPRING_MAIL_HOST=smtp.gmail.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_USERNAME=your@email.com
export SPRING_MAIL_PASSWORD=app_password
export DB_URL=jdbc:postgresql://prod-db.com:5432/ordering_db
export DB_USER=db_user
export DB_PASSWORD=db_password

# Start application
java -jar target/ordering-system-0.0.1-SNAPSHOT.jar

# Or with Kubernetes
kubectl apply -f k8s/deployment.yaml
```

---

## 6️⃣ Logging & Monitoring

### Development Logs
```bash
# Tail logs
tail -f target/ordering-system.log

# Filter by level
grep "ERROR" target/ordering-system.log
grep "WARN" target/ordering-system.log

# Audit trail
grep "AUDIT:" target/ordering-system.log
```

### Production Logs
```bash
# Enabled with:
# server.ssl.enabled: false
# logging.file.name: /var/log/orderapp/ordering-system.log
# logging.file.max-size: 100MB
# logging.file.max-history: 30

tail -f /var/log/orderapp/ordering-system.log

# Or use Elasticsearch/Kibana for centralized logging
```

---

## 7️⃣ Configuration Files

### Development (application-dev.yaml)
```yaml
spring:
  cache:
    type: simple              # In-memory cache
  datasource:
    url: jdbc:postgresql://localhost:5432/ordering_db
    username: postgres
  springdoc:
    swagger-ui:
      enabled: true

logging:
  level:
    com.orderapp.ordering: DEBUG
```

### Production (application-prod.yaml)
```yaml
spring:
  cache:
    type: redis               # Redis cache
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}

logging:
  level:
    com.orderapp.ordering: INFO
  file:
    name: /var/log/orderapp/ordering-system.log
```

---

## 🐛 Troubleshooting

### Build Errors
```bash
# Clean cache
mvn clean

# Check Java version
java -version  # Should be 17+

# Check Maven version
mvn --version  # Should be 3.9+
```

### Compilation Issues
```bash
# Recompile with debug
mvn clean compile -X

# Check for missing dependencies
mvn dependency:tree
```

### Test Failures
```bash
# Run specific test with output
mvn test -Dtest=AdminTenantServiceTest -e

# Enable debugging
mvn test -Dtest=AdminTenantServiceTest -DdebugJunit=true
```

### Runtime Issues
```bash
# Check if port 8080 is in use
lsof -i :8080

# Database connection issues
# Check PostgreSQL running: psql -l

# Redis issues (prod only)
# redis-cli ping → Should output PONG
```

---

## ✅ Implementation Checklist

- [x] Paginazione AdminTenantService
- [x] Caching (in-memory + Redis)
- [x] Logging strutturato (Audit trail)
- [x] Validazione input rigorosa
- [x] Error handling centralizzato
- [x] 29 Unit test (100% pass)
- [x] OpenAPI/Swagger documentation
- [x] Security configuration (CORS + CSRF + JWT)
- [x] Input validation migliorata
- [x] Async operations (CompletableFuture)
- [x] Health check endpoints
- [x] Build JAR (82MB)

---

## 📚 Documentation Links

- [OpenAPI Spec](http://localhost:8080/v3/api-docs)
- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

---

## 🎯 Next Steps (Wave 3)

1. **Metriche Prometheus** - Observability dashboard
2. **Integration Tests** - Testcontainers + PostgreSQL
3. **Rate Limiting** - Prevent abuse
4. **Request Logging** - Centralized audit
5. **Performance Tuning** - Query optimization

---

Generated: 13 Maggio 2026
Build Status: ✅ SUCCESS
Test Status: ✅ 29/29 PASS
JAR Status: ✅ READY (82MB)
