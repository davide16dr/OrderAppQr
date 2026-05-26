# 🚀 Guida di Setup Completo - Flusso di Login

## 📋 Indice

1. [Prerequisiti](#prerequisiti)
2. [Setup Database](#setup-database)
3. [Setup Backend](#setup-backend)
4. [Setup Frontend](#setup-frontend)
5. [Verifica del Funzionamento](#verifica-del-funzionamento)
6. [Troubleshooting](#troubleshooting)

---

## 🔧 Prerequisiti

Prima di iniziare, assicurati di avere installato:

### Backend
- ✅ **Java 21+** (`java -version`)
- ✅ **Maven 3.8+** (`mvn -version`)
- ✅ **PostgreSQL 14+** (`psql --version`)
- ✅ **Git** (`git --version`)

### Frontend
- ✅ **Node.js 18+** (`node --version`)
- ✅ **npm 9+** (`npm --version`)
- ✅ **Angular CLI 21+** (`ng version`)

---

## 🗄️ Setup Database

### 1. Crea il database PostgreSQL

```bash
# Connettiti a PostgreSQL
psql -U postgres

# Crea il database
CREATE DATABASE ordering_db;

# Crea l'utente (se non esiste)
CREATE USER orderapp WITH PASSWORD 'orderapp123';

# Assegna permessi
GRANT ALL PRIVILEGES ON DATABASE ordering_db TO orderapp;

# Esci
\q
```

### 2. Verifica la connessione

```bash
psql -U orderapp -d ordering_db -h localhost
```

### 3. Le migrazioni Flyway verranno eseguite automaticamente

Quando avvii il backend, Flyway eseguirà automaticamente:
- `V1__init.sql` - Schema iniziale
- `V2__auth_improvements.sql` - Aggiunte autenticazione

---

## ⚙️ Setup Backend (Spring Boot)

### 1. Naviga alla cartella backend

```bash
cd /Users/davideranavolo/Desktop/orderApp/OrderApp-1/ordering-system
```

### 2. Configura application.yaml

Apri `src/main/resources/application.yaml` e verifica:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ordering_db
    username: orderapp
    password: orderapp123

app:
  jwt:
    secret: OrderAppSecretKeyForJWTTokenGenerationAndValidation2024OrderQRSuperSecureKey
    expiration: 86400000
    refreshExpiration: 604800000
```

### 3. Installa le dipendenze

```bash
mvn clean install
```

### 4. Avvia il backend

```bash
mvn spring-boot:run
```

**Output atteso:**
```
Started OrderingSystemApplication in 5.234 seconds
Server is running on http://localhost:8080
```

### 5. Verifica il backend

```bash
curl http://localhost:8080/api/public/auth/login
```

Dovrebbe ritornare un errore 405 (Method Not Allowed) poiché è POST only:
```json
{"error": "Method Not Allowed"}
```

---

## 💻 Setup Frontend (Angular)

### 1. Naviga alla cartella frontend

```bash
cd /Users/davideranavolo/Desktop/orderApp/OrderApp-1/ordering-frontend
```

### 2. Installa le dipendenze

```bash
npm install
```

### 3. Verifica l'API URL

Apri `src/environments/environment.ts` e assicurati che:

```typescript
export const environment = {
  apiUrl: 'http://localhost:8080/api'
};
```

### 4. Avvia il development server

```bash
ng serve
# oppure
npm start
```

**Output atteso:**
```
✔ Compiled successfully.
● Angular Live Development Server is listening on localhost:4200
```

### 5. Apri il browser

Naviga a: `http://localhost:4200`

---

## ✅ Verifica del Funzionamento

### 1. Accedi alla pagina di login

```
http://localhost:4200/public/login
```

Dovresti vedere:
- ✅ Sidebar sinistra con branding "OrderQR"
- ✅ Form login con email e password
- ✅ Pulsante "Accedi" disabilitato
- ✅ Link "Registra la tua attività"

### 2. Testa la validazione del form

Prova a:
- Digitare un'email non valida → errore "Email non valida"
- Digitare una password < 6 caratteri → errore "Minimo 6 caratteri"
- Lasciare campi vuoti → errore "Campo obbligatorio"

Nota: Il pulsante rimane disabilitato finché il form non è valido.

### 3. Crea un utente di test nel database

```sql
-- Connettiti al database
psql -U orderapp -d ordering_db

-- Inserisci uno staff user di test
INSERT INTO staff_users (tenant_id, first_name, last_name, email, password_hash, status, is_primary_contact, created_at, updated_at)
VALUES (
  1,
  'Giovanni',
  'Rossi',
  'giovanni.rossi@azienda.it',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36P4/KFm', -- bcrypt: 'password123'
  'ACTIVE',
  true,
  NOW(),
  NOW()
);
```

**Password di test:** `password123`

### 4. Effettua il login

Nel form, inserisci:
- **Email:** `giovanni.rossi@azienda.it`
- **Password:** `password123`

Clicca "Accedi"

**Atteso:**
- ✅ Loading spinner nel pulsante
- ✅ Reindirizzamento a `/staff/dashboard`
- ✅ Dashboard mostra il nome utente

### 5. Verifica nel browser DevTools

**Tab Network:**
- Dovresti vedere una richiesta POST a `/api/public/auth/login`
- Response contiene `accessToken`, `refreshToken`, e `user` data

**Tab Storage → localStorage:**
- `auth_token` → JWT access token
- `refresh_token` → JWT refresh token
- `auth_user` → User data in JSON

**Tab Console:**
- Nessun errore CORS
- Nessun errore di validazione JWT

### 6. Testa i link della dashboard

Nella dashboard:
- Clicca i card per navigare
- Clicca il pulsante "Esci" per logout
- Verifica che localStorage venga pulito
- Reindirizzamento a `/public/login`

### 7. Testa il "Ricordami"

1. Torna al login
2. Seleziona "Ricordami su questo dispositivo"
3. Effettua il login
4. Vai a localStorage → `staff_remembered_email` dovrebbe contenere l'email
5. Ricarica la pagina → email dovrebbe essere pre-compilata

---

## 🔍 Test Scenari Avanzati

### Scenario 1: Password Sbagliata

```
Email: giovanni.rossi@azienda.it
Password: passwordSbagliata123
```

**Atteso:**
- ❌ Error alert: "Email o password non corretti"
- Animazione shake dell'alert
- Pulsante "Accedi" rimane abilitato per retry

### Scenario 2: Email Non Registrata

```
Email: nonesiste@azienda.it
Password: password123
```

**Atteso:**
- ❌ Error alert: "Email o password non corretti"
- ❌ Non rivela se l'email esiste o no (sicurezza)

### Scenario 3: Account Disabilitato

Inserisci nel database un utente con `status = 'INACTIVE'`:

```sql
INSERT INTO staff_users (tenant_id, first_name, last_name, email, password_hash, status, created_at, updated_at)
VALUES (1, 'Marco', 'Bianchi', 'marco@azienda.it', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36P4/KFm', 'INACTIVE', NOW(), NOW());
```

Tenta il login:
- ❌ Error alert: "Account disabilitato o sospeso"

### Scenario 4: Refresh Token

1. Attendi che l'access token scada (in test: modifica l'expiration)
2. Effettua una nuova richiesta API
3. L'interceptor dovrebbe automaticamente fare refresh
4. localStorage dovrebbe avere nuovi token

### Scenario 5: Accesso Diretto a Rotte Protette

Tenta di accedere a:
```
http://localhost:4200/staff/dashboard
```

Senza essere loggato:
- ✅ authGuard redirige a `/public/login`
- ✅ queryParams contiene `returnUrl=/staff/dashboard`

---

## 🛠️ Troubleshooting

### Errore: "Connection refused" al database

**Problema:** PostgreSQL non è avviato

**Soluzione:**
```bash
# macOS
brew services start postgresql

# Linux
sudo systemctl start postgresql

# Windows
net start PostgreSQL14
```

### Errore: "FATAL: database ordering_db does not exist"

**Soluzione:** Crea il database seguendo [Setup Database](#setup-database)

### Errore: CORS "Access to XMLHttpRequest blocked"

**Problema:** Frontend su http://localhost:4200 non può raggiungere backend

**Soluzione:**
- Verifica che il backend è in esecuzione su http://localhost:8080
- Verifica che SecurityConfig ha CORS configurato per http://localhost:4200
- Verifica la console del backend per errori

### Errore: "401 Unauthorized" su richieste protette

**Problema:** Token JWT non valido o scaduto

**Soluzione:**
- Verifica che il token è salvato in localStorage
- Verifica che l'Authorization header contiene "Bearer {token}"
- Controlla che la firma JWT è valida (check JwtTokenProvider secret)
- Prova a fare di nuovo il login

### Errore: "The ngIf directive must be associated with the CommonModule"

**Problema:** Mancanza di CommonModule in imports

**Soluzione:** Assicurati che ogni component standalone includa CommonModule negli imports:
```typescript
imports: [CommonModule, ReactiveFormsModule]
```

### Errore: "Can't resolve FormFieldComponent"

**Problema:** Il componente FormFieldComponent non è importato

**Soluzione:** Aggiungi agli imports:
```typescript
imports: [FormFieldComponent]
```

---

## 📊 Comandi Utili

### Backend

```bash
# Avvia con reload hot
mvn spring-boot:run -Dspring-boot.run.watch=true

# Avvia con debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

# Pulisci e installa
mvn clean install

# Run tests
mvn test

# Build production JAR
mvn clean package
```

### Frontend

```bash
# Avvia dev server con watch mode
ng serve --open

# Build production
ng build --configuration production

# Run tests
ng test

# Run e2e tests
ng e2e

# Verifica codice (lint)
ng lint
```

---

## 🔐 Configurazione Sicurezza per Produzione

### 1. JWT Secret

❌ **Non usare:** `OrderAppSecretKeyForJWTTokenGenerationAndValidation2024OrderQRSuperSecureKey`

✅ **Usa:**
```bash
# Genera una chiave sicura
openssl rand -base64 32
```

Risultato esempio:
```
xKj8mP2nQ5rT7vW9xZaB3cD6eF9gH2jK4lM6nO8pQ0sR2tU4vW6xY8zA0bC2dE
```

Salva in environment variable:
```bash
export JWT_SECRET=xKj8mP2nQ5rT7vW9xZaB3cD6eF9gH2jK4lM6nO8pQ0sR2tU4vW6xY8zA0bC2dE
```

### 2. CORS

In produzione, modifica SecurityConfig:
```java
configuration.setAllowedOrigins(Arrays.asList("https://tuodominio.com"));
```

### 3. HTTPS

Abilita HTTPS in application.yaml:
```yaml
server:
  ssl:
    key-store: keystore.p12
    key-store-password: password
    key-store-type: PKCS12
```

### 4. Password Storage

❌ **Non usare mai:** Plain text password

✅ **Usa sempre:** BCrypt con salt
```java
passwordEncoder.encode("password123")
// → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36P4/KFm
```

---

## 📚 File Creati

### Frontend
```
src/app/
├── core/
│   ├── guards/auth-guard.ts
│   ├── interceptors/auth-interceptor.ts
│   └── services/auth.service.ts
├── features/
│   ├── public/
│   │   ├── pages/staff-login/
│   │   │   ├── staff-login.component.ts
│   │   │   ├── staff-login.component.html
│   │   │   └── staff-login.component.scss
│   │   └── public.routes.ts
│   └── staff/
│       └── pages/staff-dashboard/
│           ├── staff-dashboard.component.ts
│           ├── staff-dashboard.component.html
│           └── staff-dashboard.component.scss
├── app.config.ts (aggiornato)
└── app.routes.ts (aggiornato)
```

### Backend
```
src/main/java/com/orderapp/ordering/
├── controller/
│   └── AuthController.java
├── dto/
│   ├── LoginRequestDTO.java
│   ├── LoginResponseDTO.java
│   └── RefreshTokenRequestDTO.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
└── service/
    └── AuthService.java

src/main/resources/
├── application.yaml (aggiornato)
└── db/migration/
    └── V2__auth_improvements.sql
```

---

## 🎯 Prossimi Step

Dopo aver completato il setup di login, puoi:

1. **Implementare Password Reset**
   - Creare endpoint `/api/public/auth/forgot-password`
   - Inviare email con link di reset

2. **Implementare 2FA (Two-Factor Authentication)**
   - TOTP con Google Authenticator
   - SMS OTP

3. **Aggiungere Role-Based Access Control (RBAC)**
   - Proteggere endpoint con @PreAuthorize("hasRole('ADMIN')")
   - Creare endpoint per staff permissions

4. **Implementare Audit Logging**
   - Log login/logout
   - Log accesso endpoint protetti
   - Tracciare IP e User-Agent

5. **Aggiungere Session Management**
   - Refresh token blacklist
   - Force logout utenti
   - Session timeout

---

## 📞 Support

Per problemi o domande:

1. Verifica il [Troubleshooting](#troubleshooting)
2. Controlla i log del backend: `target/spring.log`
3. Apri console browser DevTools (F12)
4. Verifica le richieste Network nella scheda Network

---

**Data Setup:** 3 Aprile 2026
**Angular Version:** 21+
**Spring Boot Version:** 3.x
**PostgreSQL Version:** 14+

