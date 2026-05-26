# �� Flusso di Login Completo - Documentazione Implementazione

## 📋 Panoramica del Flusso

Ho implementato un **flusso di login completo** seguendo lo stile moderno di Angular 21 e Spring Boot, con validazione dati, gestione token JWT, e accesso al backend con database.

---

## 🎯 Architettura del Sistema

### **Frontend (Angular 21)**

```
┌─────────────────────────────────────────────────────────────┐
│                    STAFF LOGIN PAGE                         │
│  - Email input con validazione                              │
│  - Password input con visibilità toggle                      │
│  - Ricordami checkbox                                        │
│  - Animazioni fluide e error handling                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    AUTH SERVICE                             │
│  - Effettua richiesta HTTP POST /api/public/auth/login      │
│  - Riceve accessToken, refreshToken, userDTO                │
│  - Salva token in localStorage                              │
│  - Usa Signal API per stato reactivo                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│              JWT AUTH INTERCEPTOR                           │
│  - Intercetta tutte le richieste HTTP                        │
│  - Aggiunge Bearer token all'Authorization header           │
│  - Filtra solo endpoint autenticati                         │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    BACKEND (Spring Boot)                    │
│  /api/public/auth/login (POST)                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 File Creati

### **Frontend - Angular 21**

#### 1. **Componente Login** (`staff-login.component.ts`)
- ✅ Signal API per state management (`isLoading`, `errorMessage`, `showPassword`)
- ✅ Reactive Forms con validazione email e password
- ✅ Gestione errori con animazioni shake
- ✅ Toggle visibilità password
- ✅ Ricordami email su localStorage
- ✅ Navigazione alla dashboard dopo login

#### 2. **Template HTML** (`staff-login.component.html`)
- ✅ Layout sidebar + form (responsive)
- ✅ Sezione branding con features list
- ✅ Form con validazione visiva
- ✅ Error alert con animazione fadeIn
- ✅ Button loading state
- ✅ Link signup e forgot password

#### 3. **Stili SCSS** (`staff-login.component.scss`)
- ✅ Design moderno con gradient background
- ✅ Animated background shapes
- ✅ Responsive design (mobile first)
- ✅ Transizioni fluide e animazioni
- ✅ Input states (focus, valid, error)

#### 4. **Auth Service** (`auth.service.ts`)
- ✅ Signal API per stato reattivo (`isAuthenticated`, `currentUser`, `isLoading`)
- ✅ Metodo `login()` che chiama l'endpoint backend
- ✅ Metodo `refreshAccessToken()` per rinnovare il token
- ✅ Metodo `logout()` che pulisce localStorage
- ✅ Validazione token JWT (decodifica scadenza)
- ✅ Ripristino sessione da localStorage
- ✅ Error handling con messaggi localizzati

#### 5. **JWT Auth Interceptor** (`auth-interceptor.ts`)
- ✅ HttpInterceptorFn moderna (Angular 21+)
- ✅ Aggiunge Bearer token all'Authorization header
- ✅ Filtra solo endpoint autenticati (/api/)
- ✅ Esclude endpoint pubblici (/api/public/)

#### 6. **Auth Guard** (`auth-guard.ts`)
- ✅ `authGuard`: protegge rotte private richiedendo autenticazione
- ✅ `publicGuard`: ridirige autenticati dal login alla dashboard
- ✅ Salva returnUrl nei queryParams

#### 7. **Staff Dashboard** (`staff-dashboard.component.ts/html/scss`)
- ✅ Mostra dati utente autenticato
- ✅ Dashboard cards (ordini, ricavi, ecc.)
- ✅ Pannello admin (solo per admin)
- ✅ Pulsante logout
- ✅ Animazioni smooth fadeIn
- ✅ Design moderno e responsivo

#### 8. **App Routes** (`app.routes.ts`)
- ✅ Route `/staff/dashboard` protetta con `authGuard`
- ✅ Public routes (login, signup) protette con `publicGuard`
- ✅ Fallback redirect

#### 9. **App Config** (`app.config.ts`)
- ✅ Registrazione `authTokenInterceptor` con `withInterceptors`
- ✅ Provider HttpClient

---

### **Backend - Spring Boot**

#### 1. **Auth Controller** (`AuthController.java`)
```
POST /api/public/auth/login
- Riceve: LoginRequestDTO (email, password)
- Ritorna: LoginResponseDTO (accessToken, refreshToken, userDTO)
- Error handling: 401 Unauthorized, 400 Bad Request, 500 Server Error

POST /api/public/auth/refresh
- Riceve: RefreshTokenRequestDTO (refreshToken)
- Ritorna: LoginResponseDTO con nuovi token
- Error handling: 401 Unauthorized
```

#### 2. **Auth Service** (`AuthService.java`)
- ✅ `login()`: valida credenziali, genera JWT token
- ✅ `refreshToken()`: valida refresh token, genera nuovo access token
- ✅ Verifica stato account (ACTIVE/INACTIVE)
- ✅ Aggiorna lastLoginAt timestamp
- ✅ Assegna ruoli basati su isPrimaryContact

#### 3. **JWT Token Provider** (`JwtTokenProvider.java`)
- ✅ `generateAccessToken()`: crea JWT con claims (email, firstName, lastName, tenantId, roles)
- ✅ `generateRefreshToken()`: crea refresh token con scadenza 7 giorni
- ✅ `validateToken()`: valida firma e scadenza JWT
- ✅ `getUserIdFromToken()`: estrae userId dal token
- ✅ HMAC-SHA512 signature

#### 4. **JWT Authentication Filter** (`JwtAuthenticationFilter.java`)
- ✅ Estrae token dall'Authorization header (Bearer token)
- ✅ Valida il token usando JwtTokenProvider
- ✅ Configura SecurityContext con autenticazione
- ✅ Esclude endpoint pubblici (`/api/public/*`)

#### 5. **Security Config** (`SecurityConfig.java`)
- ✅ Configura SecurityFilterChain
- ✅ Aggiunge JwtAuthenticationFilter
- ✅ Disabilita CSRF (stateless JWT)
- ✅ SessionCreationPolicy.STATELESS
- ✅ Configura CORS (localhost:4200, localhost:3000)
- ✅ PasswordEncoder BCrypt
- ✅ Autorizzazione: `/api/public/**` permesso a tutti, resto richiede autenticazione

#### 6. **DTO Classes**
- `LoginRequestDTO`: email (email valida), password (min 6 char)
- `LoginResponseDTO`: accessToken, refreshToken, userDTO
- `RefreshTokenRequestDTO`: refreshToken
- `LoginResponseDTO.UserDTO`: id, email, firstName, lastName, tenantId, roles

#### 7. **Database Migration** (`V2__auth_improvements.sql`)
- ✅ Colonna `password_hash` (BCrypt)
- ✅ Colonna `is_primary_contact` (boolean)
- ✅ Colonna `last_login_at` (timestamp)
- ✅ Indici su email per query veloci
- ✅ Tabella `refresh_tokens` (opzionale, per blacklist)

#### 8. **Application Config** (`application.yaml`)
```yaml
app:
  jwt:
    secret: OrderAppSecretKeyForJWTTokenGenerationAndValidation2024OrderQRSuperSecureKey
    expiration: 86400000  # 24 hours
    refreshExpiration: 604800000  # 7 days
```

---

## 🔄 Flusso di Login - Step by Step

### **1. Utente accede alla pagina login**
```
Frontend: /public/login (protetta da publicGuard)
```

### **2. Utente inserisce credenziali e clicca "Accedi"**
```typescript
// staff-login.component.ts
onLogin() {
  const credentials: LoginRequest = { email, password };
  authService.login(credentials).subscribe(...)
}
```

### **3. Frontend invia richiesta al backend**
```
POST /api/public/auth/login
Body: { email: "staff@azienda.it", password: "password123" }
Header: Content-Type: application/json
```

### **4. Backend valida e genera token**
```java
// AuthController.java - /api/public/auth/login
public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest) {
    LoginResponseDTO response = authService.login(loginRequest);
    // Valida email/password, genera JWT token
    return ResponseEntity.ok(response);
}
```

### **5. Backend ritorna i token**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "1",
    "email": "staff@azienda.it",
    "firstName": "Giovanni",
    "lastName": "Rossi",
    "tenantId": "123",
    "roles": ["STAFF", "ADMIN"]
  }
}
```

### **6. Frontend salva i token e naviga**
```typescript
// auth.service.ts
handleAuthSuccess(response: LoginResponse) {
  localStorage.setItem('auth_token', response.accessToken);
  localStorage.setItem('refresh_token', response.refreshToken);
  localStorage.setItem('auth_user', JSON.stringify(response.user));
  isAuthenticated.set(true);
  currentUser.set(response.user);
}
// Naviga a /staff/dashboard
router.navigate(['/staff/dashboard']);
```

### **7. Richieste successive aggiungono il token**
```typescript
// auth-interceptor.ts
if (token && isAuthUrl(req)) {
  req = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}
```

### **8. Backend valida il token in ogni richiesta**
```java
// JwtAuthenticationFilter.java
String token = extractTokenFromRequest(request);
if (jwtTokenProvider.validateToken(token)) {
  String userId = jwtTokenProvider.getUserIdFromToken(token);
  // Configura SecurityContext
  SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

### **9. Refresh Token (quando access token scade)**
```
POST /api/public/auth/refresh
Body: { refreshToken: "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..." }
```

### **10. Logout**
```typescript
// auth.service.ts
logout() {
  localStorage.removeItem('auth_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('auth_user');
  isAuthenticated.set(false);
  currentUser.set(null);
  router.navigate(['/public/login']);
}
```

---

## 🔒 Sicurezza Implementata

### **Frontend**
- ✅ Validazione email con regex
- ✅ Password minimo 6 caratteri
- ✅ Token salvato in localStorage (⚠️ considerare sessionStorage per più sicurezza)
- ✅ JWT decodifica lato client per verificare scadenza
- ✅ Interceptor aggiunge Bearer token solo a endpoint autenticati
- ✅ Guard protegge rotte private

### **Backend**
- ✅ BCrypt password hashing (non plain text)
- ✅ JWT firma con HMAC-SHA512
- ✅ Token scadenza (access: 24h, refresh: 7 giorni)
- ✅ Validazione JWT firma e scadenza
- ✅ CORS configurato (whitelist localhost:4200)
- ✅ CSRF disabilitato (stateless JWT)
- ✅ Verifica stato account (ACTIVE/INACTIVE)
- ✅ Indici database per performance query login
- ✅ Error messages localizzati (non espone dettagli interni)

---

## 📊 Database Schema

### **staff_users table**
```sql
id (BIGINT PRIMARY KEY)
tenant_id (BIGINT NOT NULL - FK)
first_name (VARCHAR)
last_name (VARCHAR)
email (VARCHAR UNIQUE)
password_hash (VARCHAR - BCrypt)
phone (VARCHAR)
is_primary_contact (BOOLEAN - per ruoli ADMIN)
status (VARCHAR - ACTIVE/INACTIVE)
last_login_at (TIMESTAMP)
invited_at (TIMESTAMP)
activated_at (TIMESTAMP)
created_at (TIMESTAMP)
updated_at (TIMESTAMP)

Indici:
- idx_staff_users_email (email)
- idx_staff_users_tenant_email (tenant_id, email)
```

### **refresh_tokens table (opzionale)**
```sql
id (BIGINT PRIMARY KEY)
staff_user_id (BIGINT FK)
token (TEXT UNIQUE)
expires_at (TIMESTAMP)
created_at (TIMESTAMP)
updated_at (TIMESTAMP)

Indici:
- idx_refresh_tokens_user_id
- idx_refresh_tokens_expires_at
```

---

## 🚀 Come Testare il Flusso

### **1. Backend - Avviare Spring Boot**
```bash
cd ordering-system
mvn spring-boot:run
```

### **2. Frontend - Avviare Angular**
```bash
cd ordering-frontend
npm start
```

### **3. Navigare al login**
```
http://localhost:4200/public/login
```

### **4. Usare credenziali di test**
```
Email: admin@azienda.it
Password: password123
```

### **5. Verificare nel browser DevTools**
- Network: richiesta POST a `/api/public/auth/login`
- Storage: localStorage con `auth_token`, `refresh_token`, `auth_user`
- Console: nessun errore

### **6. Navigare alla dashboard**
```
http://localhost:4200/staff/dashboard
```

---

## ⚠️ Note Importanti

### **1. Password di Default**
Nel database, la colonna `password_hash` ha una password BCrypt dummy. Gli utenti veri devono:
- Impostare una password durante l'onboarding
- Usare il sistema di "Password Dimenticata" per reset

### **2. JWT Secret**
La chiave segreta JWT in `application.yaml` deve:
- ✅ Essere lunga (64+ caratteri)
- ✅ Essere unica per ogni ambiente
- ✅ Non essere committata nel git (usare environment variables)
- ✅ Essere conservata in un vault sicuro (AWS Secrets Manager, etc.)

### **3. CORS**
La configurazione CORS è limitata a:
- `http://localhost:4200`
- `http://localhost:3000`
- `http://127.0.0.1:4200`

In produzione, aggiungere solo i domini reali.

### **4. Token Storage**
⚠️ **localStorage** è vulnerabile a XSS. Considerare:
- Usare **httpOnly cookies** (più sicuro)
- Oppure **sessionStorage** (cancellato quando chiudi browser)
- Implementare CSP (Content Security Policy)

### **5. Refresh Token Strategy**
Attualmente, il refresh token è memorizzato nel JWT stesso. Per maggiore sicurezza:
- Salvare refresh token nel database
- Implementare blacklist di token revocati
- Usare rolling refresh token

---

## 📝 Checklist di Implementazione

- ✅ Componente login frontend
- ✅ Template HTML responsive
- ✅ Stili SCSS moderni
- ✅ Auth Service con Signal API
- ✅ JWT Interceptor
- ✅ Auth Guard (protezione rotte)
- ✅ Staff Dashboard
- ✅ Auth Controller backend
- ✅ Auth Service backend
- ✅ JWT Token Provider
- ✅ JWT Authentication Filter
- ✅ Security Config
- ✅ DTO classes
- ✅ Database migration
- ✅ application.yaml config
- ✅ App routes aggiornate
- ✅ App config con interceptor

---

## 🔗 Endpoint Pubblici

```
POST /api/public/auth/login
- Richiede: email, password
- Ritorna: accessToken, refreshToken, userDTO

POST /api/public/auth/refresh
- Richiede: refreshToken
- Ritorna: accessToken, refreshToken, userDTO
```

---

## 🔐 Endpoint Protetti (richiedono Bearer token)

```
GET /api/staff/dashboard
GET /api/staff/profile
PUT /api/staff/profile
POST /api/staff/logout (opzionale)
... altri endpoint staff
```

---

## 🎨 Design System Coerente

Tutti i componenti seguono lo stesso stile moderno di Angular 21:
- ✅ Gradient backgrounds lineari
- ✅ Card components con shadow e hover effects
- ✅ Input fields con validazione visiva
- ✅ Animazioni fluide (@if, @for, @switch)
- ✅ Signal API per reattività
- ✅ Responsive design mobile-first
- ✅ Emoji per icone (semplice e moderno)
- ✅ Colori coerenti (indigo, green, red)

---

## 📚 Documentazione Correlata

Vedere anche:
- `REGISTRAZIONE_IMPLEMENTAZIONE.md` - Flusso business-signup
- `database_explanation_detailed.txt` - Schema database completo
- `SETUP_GUIDA.md` - Guida setup progetto

