# Guida di Setup - Flusso di Registrazione Aziendale

## ✅ Checklist di Implementazione Completata

### Backend (Spring Boot)
- [x] DTO: `BusinessSignupRequest.java` con validazioni Bean Validation
- [x] DTO: `BusinessSignupResponse.java`
- [x] Entità: `Tenant.java` (aggiornata con tutti i campi)
- [x] Entità: `BusinessRegistrationRequest.java`
- [x] Entità: `StaffUser.java`
- [x] Entità: `StaffRole.java`
- [x] Entità: `StaffUserRole.java`
- [x] Entità: `TenantSubscription.java`
- [x] Repository: `BusinessRegistrationRequestRepository`
- [x] Repository: `TenantRepository`
- [x] Repository: `StaffUserRepository`
- [x] Repository: `StaffUserRoleRepository`
- [x] Repository: `StaffRoleRepository`
- [x] Repository: `TenantSubscriptionRepository`
- [x] Service: `BusinessRegistrationService` con logica transazionale
- [x] Controller: `BusinessRegistrationController` con endpoint REST
- [x] Config: `SecurityConfig.java` con BCryptPasswordEncoder
- [x] Config: `CorsConfig.java` per abilitare CORS
- [x] Config: `GlobalExceptionHandler.java` per gestione errori

### Frontend (Angular)
- [x] Servizio: `BusinessRegistrationService.ts`
- [x] Componente: `BusinessSignupComponent.ts` (multi-step form)
- [x] Template: `business-signup.component.html`
- [x] Styling: `business-signup.component.scss` (responsive + animazioni)
- [x] Componente: `SignupSuccessComponent.ts`
- [x] Template: `signup-success.component.html`
- [x] Styling: `signup-success.component.scss`
- [x] Routing: `public.routes.ts`
- [x] Routing: `app.routes.ts` (aggiornata)
- [x] Environment: `environment.ts`

### Documentazione
- [x] `REGISTRAZIONE_IMPLEMENTAZIONE.md` - Documentazione completa
- [x] `SETUP_GUIDA.md` - Questa guida

---

## 🚀 Istruzioni di Setup

### Prerequisiti
- Node.js v18+
- npm v9+
- Java 21+
- Maven 3.9+
- PostgreSQL 13+

### Step 1: Configurare il Database PostgreSQL

```bash
# 1. Connettersi a PostgreSQL
psql -U postgres

# 2. Creare il database
CREATE DATABASE ordering_db;

# 3. Connettersi al database
\c ordering_db

# 4. Eseguire lo schema SQL
\i schema.sql

# 5. Verificare che le tabelle siano state create
\dt
```

La Flyway migration auto-eseguirà lo schema all'avvio dell'applicazione Spring Boot.

### Step 2: Configurare il Backend Spring Boot

```bash
# 1. Navigare nella directory del progetto
cd /Users/davideranavolo/Desktop/orderApp/OrderApp-1/ordering-system

# 2. Compilare il progetto Maven
mvn clean install

# 3. Avviare l'applicazione Spring Boot
mvn spring-boot:run
```

L'applicazione sarà disponibile su `http://localhost:8080`

**Verificare che l'applicazione sia avviata:**
```bash
curl http://localhost:8080/actuator/health
```

### Step 3: Configurare il Frontend Angular

```bash
# 1. Navigare nella directory del frontend
cd /Users/davideranavolo/Desktop/orderApp/OrderApp-1/ordering-frontend

# 2. Installare le dipendenze npm
npm install

# 3. Avviare il dev server Angular
ng serve

# Oppure
npm start
```

L'applicazione sarà disponibile su `http://localhost:4200`

### Step 4: Popolare i Dati Iniziali (Seeds)

Eseguire queste query SQL per popolare i ruoli iniziali:

```sql
-- Eseguire nel database ordering_db
INSERT INTO staff_roles (code, description) VALUES 
  ('MANAGER', 'Referente o gestore del tenant'),
  ('BAR', 'Operatore bar'),
  ('KITCHEN', 'Operatore cucina'),
  ('RUNNER', 'Addetto consegna');
```

---

## �� Test del Flusso di Registrazione

### Via Interface Web

1. **Navigare a:** `http://localhost:4200/public/signup`

2. **Step 1 - Informazioni Aziendali:**
   - Nome Azienda: "La Spiaggia Dorata"
   - Ragione Sociale: "La Spiaggia Dorata S.R.L."
   - Tipo di Attività: "LIDO"
   - Email Aziendale: "info@spiaggia.it"

3. **Step 2 - Indirizzo:**
   - Indirizzo: "Via del Mare 123"
   - Città: "Roma"
   - Provincia: "RM"
   - CAP: "00100"
   - Paese: "Italy"

4. **Step 3 - Contatti e Credenziali:**
   - Nome: "Giovanni"
   - Cognome: "Rossi"
   - Email Contatto: "giovanni@spiaggia.it"
   - Password: "SecurePassword123"
   - Conferma Password: "SecurePassword123"
   - Slug: "la-spiaggia-dorata"
   - Piano: "BASIC"

5. **Cliccare "Registrati"**

### Via API REST (cURL)

```bash
curl -X POST http://localhost:8080/api/public/business-registration/signup \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "La Spiaggia Dorata",
    "legalName": "La Spiaggia Dorata S.R.L.",
    "businessType": "LIDO",
    "vatNumber": "IT12345678901",
    "businessEmail": "info@spiaggia.it",
    "businessPhone": "+39 06 12345678",
    "addressLine1": "Via del Mare 123",
    "addressLine2": "",
    "city": "Roma",
    "province": "RM",
    "postalCode": "00100",
    "country": "Italy",
    "contactFirstName": "Giovanni",
    "contactLastName": "Rossi",
    "contactEmail": "giovanni@spiaggia.it",
    "contactPhone": "+39 333 1234567",
    "password": "SecurePassword123",
    "confirmPassword": "SecurePassword123",
    "requestedSlug": "la-spiaggia-dorata",
    "requestedPlanCode": "BASIC",
    "billingCycle": "MONTHLY"
  }'
```

### Risposta Attesa (Successo)

```json
{
  "tenantId": 1,
  "tenantSlug": "la-spiaggia-dorata",
  "tenantStatus": "PENDING",
  "message": "Registrazione completata con successo. Il tuo account è in attesa di attivazione."
}
```

---

## 🔍 Verifica dei Dati nel Database

Dopo una registrazione riuscita, verificare i dati:

```sql
-- Verificare il tenant creato
SELECT id, slug, name, status FROM tenants WHERE slug = 'la-spiaggia-dorata';

-- Verificare lo staff user (contatto primario)
SELECT id, tenant_id, first_name, last_name, email, is_primary_contact 
FROM staff_users WHERE email = 'giovanni@spiaggia.it';

-- Verificare il ruolo assegnato
SELECT sur.staff_user_id, sr.code 
FROM staff_user_roles sur
JOIN staff_roles sr ON sur.role_id = sr.id
WHERE sur.staff_user_id = 1;

-- Verificare la subscription
SELECT id, tenant_id, plan_code, status 
FROM tenant_subscriptions WHERE tenant_id = 1;

-- Verificare la registration request
SELECT id, tenant_id, status 
FROM business_registration_requests WHERE tenant_id = 1;
```

---

## ⚠️ Troubleshooting

### Errore: "La partita IVA non può superare 50 caratteri"
**Soluzione:** Verificare che il campo VAT number sia corretto nel form

### Errore: "Lo slug è già in uso"
**Soluzione:** Lo slug deve essere unico. Usare uno slug diverso come "la-mia-spiaggia-dorata"

### Errore: "Le password non coincidono"
**Soluzione:** Assicurarsi che password e conferma password siano identiche

### Errore CORS: "Access to XMLHttpRequest has been blocked"
**Soluzione:** Verificare che `CorsConfig.java` sia configurato correttamente e che Spring Boot sia in esecuzione

### Errore: "Impossibile connettersi al database"
**Soluzione:** 
- Verificare che PostgreSQL sia in esecuzione
- Controllare le credenziali nel file `application.yaml`
- Assicurarsi che il database `ordering_db` esista

### Errore: "Nessun bean di tipo 'PasswordEncoder' trovato"
**Soluzione:** Verificare che `SecurityConfig.java` sia nel path di scansione di Spring (`@Configuration`)

---

## 📚 Documentazione Risorsa

### Backend Endpoints

**POST /api/public/business-registration/signup**
- Descrizione: Registra una nuova azienda
- Headers: `Content-Type: application/json`
- Body: `BusinessSignupRequest`
- Response: `BusinessSignupResponse`
- Status Code: 201 (Created) o 400 (Bad Request)

**POST /api/public/business-registration/{tenantId}/approve**
- Descrizione: Approva una registrazione (solo admin)
- Query Params: `approvedByStaffUserId` (required)
- Response: `BusinessSignupResponse`
- Status Code: 200 (OK) o 400 (Bad Request)

### Frontend Routes

- **GET /public/signup** → Pagina di registrazione
- **GET /public/signup-success** → Pagina di successo

---

## �� Security Considerations

1. **Password Hashing:** Le password sono hashate con BCrypt (cost factor: 10)
2. **CORS:** Configurato solo per localhost in sviluppo
3. **Validazione Input:** Bean Validation su tutti i DTOs
4. **SQL Injection:** Protetto tramite JPA parameterized queries
5. **HTTPS:** In produzione, usare HTTPS e abilitare HSTS

---

## 📊 File di Migrazione Flyway

La Flyway è configurata per eseguire automaticamente le migrazioni da:
```
src/main/resources/db/migration/
```

Nel nostro caso:
```
V1__init.sql (contiene lo schema completo dal file schema.sql)
```

---

## 🎯 Prossimi Step Consigliati

1. **Implementare il Login:**
   - Creare endpoint di autenticazione per staff users
   - Implementare JWT tokens
   - Creare componente login Angular

2. **Aggiungere Email Service:**
   - Inviare email di conferma registrazione
   - Inviare email di approvazione

3. **Implementare Dashboard Admin:**
   - Visualizzare registrazioni pendenti
   - Approvare/rifiutare registrazioni

4. **Aggiungere Tests:**
   - Unit tests per il service
   - Integration tests per gli endpoint
   - E2E tests per il frontend

5. **Setup CI/CD:**
   - GitHub Actions o GitLab CI
   - Automated testing
   - Automated deployments

---

## 📞 Support

In caso di problemi:

1. Verificare i log di Spring Boot: `tail -f target/logs/application.log`
2. Verificare la console del browser (F12) per errori JavaScript
3. Verificare che tutte le dipendenze Maven siano scaricate
4. Eseguire `mvn clean install` per pulire e ricompilare
5. Eseguire `npm install --force` per forzare la reinstallazione delle dipendenze

---

## ✨ Conclusione

L'implementazione del flusso di registrazione aziendale è completa! 

La piattaforma è pronta per:
- ✅ Accettare nuove registrazioni di aziende
- ✅ Validare completamente i dati di input
- ✅ Creare tenant, staff users e subscriptions atomicamente
- ✅ Mantenere i tenant in stato PENDING fino all'approvazione manuale
- ✅ Tracciare tutte le registrazioni per audit

Buon lavoro! 🚀
