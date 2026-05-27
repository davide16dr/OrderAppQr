# 🚀 Deployment Guide - OrderApp Online (Vercel + Render)

**Generated:** 14 Maggio 2026  
**Status:** Ready for Online Deployment

---

## 📋 Pre-Deployment Checklist

- [x] Backend configured for production (Spring Boot Render)
- [x] Frontend configured for Vercel deployment
- [x] Environment variables template created (.env.example)
- [x] Database configuration prepared
- [x] Redis cache configured
- [x] JWT security setup
- [x] CORS properly configured

---

## 🔄 STEP-BY-STEP DEPLOYMENT

### PHASE 1: Backend Setup (Render.com)

#### 1.1 Create Render Account
- Go to https://render.com
- Sign up with GitHub account
- Authorize Render to access repositories

#### 1.2 Connect GitHub Repository
```bash
# Push your code to GitHub (if not already done)
git add .
git commit -m "prepare for deployment: add render.yaml, vercel.json, .env.example"
git push origin main
```

#### 1.3 Deploy Backend on Render
1. From Render Dashboard: **New** → **Web Service**
2. Connect GitHub repository
3. Select repository: `OrderApp-1`
4. Fill in details:
   - **Name:** `orderapp-backend`
   - **Runtime:**` Java`
   - **Region:** Choose closest to users
   - **Build Command:** `mvn clean package -DskipTests`
   - **Start Command:** `java -jar target/ordering-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

#### 1.4 Add PostgreSQL Database
1. In Render Dashboard: **New** → **PostgreSQL**
2. Configure:
   - **Name:** `orderapp_db`
   - **Database Name:** `orderapp_db`
   - **User:** `orderapp_user`
   - **Region:** Same as web service
   - **PostgreSQL Version:** `14`

3. After creation, copy the **Internal URL** (it will be auto-filled as `DB_URL`)

#### 1.5 Add Redis Cache
1. In Render Dashboard: **New** → **Redis**
2. Configure:
   - **Name:** `redis-orderapp`
   - **Region:** Same as web service
   - **Maxmemory Policy:** `allkeys-lru`

3. Copy the **Internal URL** for `REDIS_HOST`

#### 1.6 Configure Environment Variables (Render Web Service)
Go to Web Service → **Environment**

**Recommended production setup: Resend transactional email API**

```
DB_URL=              [Auto from Database connection]
DB_USER=             [Auto from Database]
DB_PASSWORD=         [Auto from Database]
REDIS_HOST=          [Auto from Cache connection]
REDIS_PORT=6379
JWT_SECRET=          [Generate: openssl rand -base64 32]
RESEND_API_KEY=      [Resend API key]
RESEND_API_ENDPOINT=https://api.resend.com/emails
APP_MAIL_FROM=       [Verified sender, e.g. OrderApp <no-reply@yourdomain.com>]
LOG_LEVEL=INFO
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
```

If you add `RESEND_API_KEY`, the backend will automatically prefer Resend unless you explicitly set `APP_MAIL_PROVIDER=smtp`. If you prefer SMTP, keep `APP_MAIL_PROVIDER=smtp` and configure `SPRING_MAIL_*` values. Resend is recommended for Render because it uses HTTPS API calls instead of outbound SMTP connectivity.

#### 1.7 Generate Secure JWT Secret
```bash
# Run locally, copy the output
openssl rand -base64 32

# Example output, use this in RENDER:
# AbCd1234efgh5678ijKl9012mnOp3456qrSt7890uvWx==
```

#### 1.8 Deploy & Wait
- Click **Deploy**
- Wait for build (usually 3-5 minutes)
- Check logs for errors
- Verify: `https://orderapp-backend.onrender.com/api/health`

**Expected Response:**
```json
{
  "status": "UP",
  "timestamp": "2026-05-14T10:30:00Z",
  "version": "1.0.0"
}
```

---

### PHASE 2: Frontend Setup (Vercel)

#### 2.1 Create Vercel Account
- Go to https://vercel.com
- Sign up with GitHub
- Authorize Vercel

#### 2.2 Import Project
1. From Vercel Dashboard: **Add New** → **Project**
2. **Import Git Repository**
3. Select your `OrderApp-1` repository
4. Configure:
   - **Framework Preset:** `Angular`
   - **Root Directory:** `ordering-frontend`
   - **Build:** `npm run build`
   - **Output:** `dist/ordering-frontend/browser`

#### 2.3 Add Environment Variables
Go to Project Settings → **Environment Variables**

```
ANGULAR_APP_API_URL=https://orderapp-backend.onrender.com
ANGULAR_APP_ENV=production
NODE_ENV=production
```

#### 2.4 Deploy & Wait
- Vercel will automatically deploy
- Build time: 1-3 minutes
- Get your URL: `https://[project-name].vercel.app`

---

### PHASE 3: Connect Frontend to Backend

#### 3.1 Update CORS on Backend
If you get CORS errors, add Vercel domain to `application-prod.yaml`:

```yaml
cors:
  allowed-origins: https://orderapp.vercel.app,http://localhost:4200
  allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
  allow-credentials: true
```

Then redeploy Render backend.

#### 3.2 Test Connection
```bash
# From your browser console:
curl https://orderapp-backend.onrender.com/api/health
curl https://[your-vercel-url]/api/health  # Should proxy to backend
```

---

### PHASE 4: Database Setup

#### 4.1 Initialize Database Schema
```bash
# SSH into Render PostgreSQL and run migrations
# Or execute locally:

# Get your DB URL from Render:
export DB_URL="postgresql://orderapp_user:password@postgres.render.com:5432/orderapp_db"

# Run Flyway migrations
mvn flyway:migrate -Dflyway.url=$DB_URL \
  -Dflyway.user=orderapp_user \
  -Dflyway.password=your_password

# Or backend handles this automatically on startup via Flyway
```

#### 4.2 Verify Database
```bash
# Connect to database
psql postgresql://orderapp_user:password@postgres.render.com:5432/orderapp_db

# Check tables
\dt

# Expected tables:
# tenants, categories, areas, orders, line_items, etc.
```

---

### PHASE 5: Testing & Verification

#### 5.1 Backend Health Checks
```bash
# Liveness check
curl https://orderapp-backend.onrender.com/api/health/live
# Response: {"alive":"true"}

# Readiness check  
curl https://orderapp-backend.onrender.com/api/health/ready
# Response: {"ready":"true"}

# Full health
curl https://orderapp-backend.onrender.com/api/health
```

#### 5.2 API Testing
```bash
# Get Swagger UI
https://orderapp-backend.onrender.com/swagger-ui.html

# Test authentication (POST)
curl -X POST https://orderapp-backend.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@orderapp.com","password":"password"}'
```

#### 5.3 Frontend Testing
1. Open `https://[your-vercel-url]`
2. Try to login with test credentials
3. Should connect to Render backend
4. Check browser Network tab for API calls

### 5.4 Check Logs

**Render Backend Logs:**
```
Dashboard → orderapp-backend → Logs
Look for:
- Flyway migrations success
- Database connection established
- Redis connection established
- Spring Boot startup completed
```

**Vercel Frontend Logs:**
```
Dashboard → Deployments → Logs
Look for:
- Build success
- No API connection errors
```

---

## 🔧 Configuration Files Reference

### render.yaml
Located at: `/render.yaml`
- Defines web service, database, cache, environment variables
- Automatically picked up by Render during build

### vercel.json
Located at: `/vercel.json`
- Defines build process for Angular
- Routes API calls to Render backend
- SPA routing configuration

### .env.example
Located at: `/.env.example`
- Template for all environment variables
- **DO NOT commit actual .env file to git**
- Copy to `.env` for local development

### application-prod.yaml
Located at: `/ordering-system/src/main/resources/application-prod.yaml`
- Production Spring Boot configuration
- Uses environment variables: `${VAR_NAME}`
- Redis cache configuration
- Database connection pooling

### environment.prod.ts
Located at: `/ordering-frontend/src/environments/environment.prod.ts`
- Angular production configuration
- API URL pointing to Render backend
- Disables debug logging

---

## 🔐 Security Checklist

- [x] JWT Secret is unique and secure (Generate with `openssl rand -base64 32`)
- [x] Database passwords are strong
- [x] Environment variables NOT committed to git
- [x] CORS is restrictive (only allow Vercel domain)
- [x] Swagger UI disabled in production config (optional)
- [x] SSL/TLS enforced (Render/Vercel handle automatically)
- [x] Database backups enabled (Render default)
- [x] Redis password secured
- [x] Mail credentials secured (App Password for Gmail)

---

## 📊 Expected URLs After Deployment

| Service | URL |
|---------|-----|
| Backend Health | https://orderapp-backend.onrender.com/api/health |
| Backend Swagger | https://orderapp-backend.onrender.com/swagger-ui.html |
| Frontend | https://orderapp.vercel.app |
| Frontend → Backend | https://orderapp-backend.onrender.com/api/* |

---

## 🐛 Troubleshooting

### Issue: "Connection refused" between Frontend and Backend

**Check:**
1. Is backend running? → Visit https://orderapp-backend.onrender.com/api/health
2. Is API URL correct in Vercel env vars?
3. Are CORS headers set? → Check backend logs

**Fix:**
```yaml
# Add to SecurityConfiguration
.cors(cors -> cors
  .allowedOrigins("https://orderapp.vercel.app", "http://localhost:4200")
  .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
  .allowCredentials(true))
```

### Issue: Database connection timeout

**Check:**
1. Is PostgreSQL addon provisioned on Render?
2. Is DB_URL correct?
3. Is password correct?

**Fix:**
```bash
# Test connection locally
psql postgresql://user:password@host:5432/db

# Check Render DB status: Dashboard → PostgreSQL → Status
```

### Issue: Redis not working

**Check:**
1. Is Redis addon running?
2. Is REDIS_HOST correct?

**Fix:**
```yaml
# In application-prod.yaml, check:
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
```

### Issue: Build fails on Render

**Check logs:**
1. Go to Render → orderapp-backend → Logs
2. Look for compilation errors
3. Check `mvn clean package` locally first

**Common causes:**
- Java version mismatch
- Missing environment variables
- Dependency download failure

---

## 🚀 Post-Deployment

### Daily Monitoring
- Check Render logs for errors: https://render.com/dashboard
- Check Vercel Analytics: https://vercel.com/dashboard
- Monitor database size: Render PostgreSQL dashboard

### Auto-Redeploy
- Every push to `main` branch deploys automatically
- Tag releases: `git tag v1.0.0 && git push --tags`

### Manual Redeploy
```bash
# Just push to main
git push origin main

# Or manually trigger in Render dashboard → Manual Deploy
```

### Scaling
- **Render:** Pro plan for production-grade uptime (99.99%)
- **Vercel:** Pro plan for priority support

---

## 📞 Support Resources

- Render Docs: https://render.com/docs
- Vercel Docs: https://vercel.com/docs
- Spring Boot Deployment: https://spring.io/guides/gs/spring-boot/
- Angular Deployment: https://angular.io/guide/deployment

---

## ✅ Deployment Complete!

Once all phases are complete:
- ✅ Backend running on Render
- ✅ Frontend running on Vercel  
- ✅ Database provisioned and populated
- ✅ Redis cache active
- ✅ CORS configured
- ✅ Health checks passing
- ✅ Ready for production use

**Next Steps:**
1. Share frontend URL with team
2. Set up custom domain (optional)
3. Configure monitoring/alerting
4. Plan backup strategy
5. Set up CI/CD pipeline improvements

---

Generated: 14 Maggio 2026
Status: ✅ READY FOR DEPLOYMENT
