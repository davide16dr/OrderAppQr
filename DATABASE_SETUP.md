# OrderApp PostgreSQL Database Setup

## 📋 Database Schema & Indexes

Il database PostgreSQL è stato ottimizzato per il tuo scenario multi-tenant.

### Files Database
```
src/main/resources/db/local/
├── schema.sql                  # Schema principale (creato automaticamente)
├── indexes-optimized.sql       # Indici strategici per performance
└── data.sql                    # Dati seed: superadmin, roles, plans
```

## 🔑 Credenziali SuperAdmin (DEVE ESSERE CAMBIATA IMMEDIATAMENTE)

### Login Iniziale
```
Email: admin@orderapp.local
Password: SecureAdmin@2024!
```

### ⚠️ Azioni Richieste
1. ✅ Login al dashboard: http://localhost:4200/login
2. ✅ **Cambiare password immediatamente**
3. ✅ Memorizzare la nuova password in modo sicuro
4. ✅ **NON** fare commit di questa password nel version control

---

## 🗄️ Database Configuration

### Local Development (PostgreSQL)
File: `application-local.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderapp
    username: orderapp_user
    password: orderapp_password
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQL10Dialect
    hibernate:
      ddl-auto: validate  # Schema already exists, validate only
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 20
```

### Alternative: Connection Pool Settings
```yaml
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

---

## 🚀 First-Time Setup Steps

### 1. Create PostgreSQL Database & User
```sql
-- Connect as superuser
CREATE USER orderapp_user WITH PASSWORD 'orderapp_password';
CREATE DATABASE orderapp WITH OWNER orderapp_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE orderapp TO orderapp_user;
ALTER SCHEMA public OWNER TO orderapp_user;

-- Connect as orderapp_user and run schema setup
\c orderapp orderapp_user
\i db/local/schema.sql
\i db/local/indexes-optimized.sql
\i db/local/data.sql
```

### 2. Verify Database Connection
From project root:
```bash
cd ordering-system

# Build project
mvn clean compile

# Run backend (will validate schema)
mvn spring-boot:run
```

Expected output:
```
[INFO] HibernateJpaVendorAdapter : HHH000412: Hibernate is in readonly mode
[INFO] Liquibase - Successfully acquired change log lock
[INFO] Application started in X.XXX seconds
```

### 3. Login to Dashboard
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- Email: `admin@orderapp.local`
- Password: `SecureAdmin@2024!` (change immediately)

---

## 📊 Database Schema Highlights

### Multi-Tenant Structure
- **tenants**: Root tenant organizations
- **staff_users**: Employees (with NULL tenant_id = global admin)
- **locations**: Points of service per tenant
- **orders**: Customer orders with snapshots

### Optimized Indexes
✅ Multi-tenant filtering (tenant_id + status combinations)
✅ Order timeline queries (created_at DESC)
✅ QR token lookups (token index)
✅ Staff email uniqueness (tenant_id, email)
✅ Location operational status (for customer menu)

### Performance Targets
- Order queries: < 100ms
- Menu catalog loads: < 200ms
- Staff dashboard: < 300ms

---

## 🔍 Verify Installation

### Check SuperAdmin Account
```sql
SELECT id, email, status, created_at FROM staff_users WHERE tenant_id IS NULL;
```

Expected: 1 row with admin@orderapp.local

### Check Demo Tenants
```sql
SELECT COUNT(*) as tenant_count FROM tenants WHERE enabled = true;
```

Expected: 6 tenants

### Check Subscription Plans
```sql
SELECT code, name, is_active FROM subscription_plans ORDER BY code;
```

Expected: BASIC, ENTERPRISE, PROFESSIONAL (all active)

---

## 🐛 Troubleshooting

### Connection Refused
```
ERROR: connection refused
```
✅ Verify PostgreSQL is running:
```bash
brew services list  # macOS
# Or
pg_isready -h localhost -p 5432
```

### Authentication Failed
```
FATAL: password authentication failed for user "orderapp_user"
```
✅ Check credentials in `application-local.yaml`
✅ Verify user exists: `psql -U postgres -c "\du orderapp_user"`

### Schema Validation Errors
```
ERROR: GenerationDisabledException: Cannot execute statements...
```
✅ Change `ddl-auto: create` in application-local.yaml (dev only!)
✅ Or manually run `db/local/schema.sql` in pgAdmin

### Indexes Not Present
```
ERROR: Missing index idx_orders_created
```
✅ Run: `\i db/local/indexes-optimized.sql` in psql

---

## 🔐 Security Notes

⚠️ **Development Only**
- Credentials in this file are for **local development only**
- Never use these credentials in production
- Change default password before any test/staging deployment

⚠️ **Production Setup**
- Use environment variables for database credentials
- Enable SSL/TLS for database connections
- Use random, strong passwords (20+ characters)
- Implement database user access controls
- Enable audit logging on staff_users table

---

## 📝 Next Steps

1. ✅ Verify database connection (your current step)
2. ✅ Login with superadmin account
3. ✅ Change superadmin password
4. ⏭️ Create production tenant accounts
5. ⏭️ Invite staff users to tenants
6. ⏭️ Configure subscription plans for each tenant

---

## 📞 Support

**Database Issues?**
- Check `logs/orderapp.log` for detailed errors
- Verify PostgreSQL version: `SELECT version();`
- Check available disk space: `SELECT pg_database_size('orderapp');`

**Performance Tuning?**
- Run: `ANALYZE;` to update statistics
- Check slow queries: `EXPLAIN ANALYZE <query>`
- Monitor query performance in logs

