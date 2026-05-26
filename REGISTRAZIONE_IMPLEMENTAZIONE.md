# Implementazione Flusso di Registrazione Aziendale

## Panoramica
Ho implementato il flusso completo di registrazione aziendale (self-signup) per la piattaforma OrderApp, seguendo i requisiti dettagliati in `registrazione.md`. L'implementazione comprende il backend Spring Boot e il frontend Angular.

## Componenti Backend Implementati

### 1. DTOs (Data Transfer Objects)

#### `BusinessSignupRequest.java`
- Contiene tutti i campi per la richiesta di registrazione aziendale
- Include validazioni Bean Validation (`@NotBlank`, `@Email`, `@Pattern`, ecc.)
- Separa i dati in tre categorie: Business, Contatto Primario, Piano & Fatturazione
- Validazione custom per password che devono coincidere

#### `BusinessSignupResponse.java`
- DTO di risposta con i dati del tenant creato
- Campi: `tenantId`, `tenantSlug`, `tenantStatus`, `message`

### 2. Entità JPA

#### `Tenant.java` (Aggiornata)
- Rappresenta un'azienda/tenant nella piattaforma
- Contiene tutti i campi dal database schema: slug, subdomain, business_type, status, ecc.
- Gestisce lo stato PENDING dopo la registrazione

#### `BusinessRegistrationRequest.java`
- Traccia le richieste di registrazione con status: SUBMITTED, APPROVED, REJECTED, CONVERTED
- Memorizza tutti i dati di registrazione per audit e tracking

#### `StaffUser.java`
- Rappresenta un utente staff (dipendente del tenant)
- Include campi per il contatto primario con is_primary_contact flag

#### `StaffRole.java`
- Entità per i ruoli dello staff (MANAGER, BAR, KITCHEN, RUNNER)

#### `StaffUserRole.java`
- Relazione many-to-many tra StaffUser e StaffRole

#### `TenantSubscription.java`
- Traccia la sottoscrizione del tenant ai piani
- Status: PENDING, TRIAL, ACTIVE, PAST_DUE, CANCELLED, EXPIRED

### 3. Repository

- `BusinessRegistrationRequestRepository`
- `TenantRepository`
- `StaffUserRepository`
- `StaffUserRoleRepository`
- `StaffRoleRepository`
- `TenantSubscriptionRepository`

### 4. Service

#### `BusinessRegistrationService.java`
Il servizio principale orchesttra l'intero flusso di registrazione:

**Metodo: `submitBusinessRegistration(request)`**
- Validazione dei dati della richiesta (password match, slug uniqueness, ecc.)
- Creazione del tenant in status PENDING
- Creazione dello staff user (contatto primario) con is_primary_contact=true
- Assegnazione del ruolo MANAGER
- Creazione della subscription del tenant
- Creazione della business_registration_request con status CONVERTED
- Tutto in una singola transazione @Transactional

**Metodo: `approveTenant(tenantId, approvedByStaffUserId)`**
- Approva un tenant da parte dei platform administrators
- Cambia lo stato da PENDING ad ACTIVE
- Imposta approved_at e activation_date

**Metodo privato: `assignManagerRole(staffUser)`**
- Assegna il ruolo MANAGER allo staff user

### 5. Controller

#### `BusinessRegistrationController.java`
Espone tre endpoint REST:

**POST /api/public/business-registration/signup**
- Endpoint pubblico per la registrazione aziendale
- Ritorna BusinessSignupResponse con tenantId e status
- Gestisce eccezioni e ritorna messaggi di errore appropriati

**POST /api/public/business-registration/{tenantId}/approve**
- Endpoint protetto per l'approvazione (da implementare con security)
- Utilizzato dai platform administrators

Include @CrossOrigin per permettere richieste dal frontend Angular.

## Componenti Frontend Implementati

### 1. Servizio Angular

#### `BusinessRegistrationService.ts`
- Interfacce TypeScript per BusinessSignupRequest e BusinessSignupResponse
- Metodo submitBusinessRegistration che comunica con il backend
- URL base configurabile via environment

### 2. Componente di Registrazione

#### `BusinessSignupComponent.ts`
- Componente standalone Angular con form reattivo (Reactive Forms)
- Flusso multi-step a 3 steps:
  - **Step 1:** Dati aziendali (nome, ragione sociale, tipo, email)
  - **Step 2:** Indirizzo (via, città, provincia, CAP, paese)
  - **Step 3:** Contatti e credenziali (nome, cognome, email, password, slug, piano)
- Validazioni complete su tutti i campi
- Messaggi di errore dinamici
- Progress bar visuale e step indicators
- Gestione dello stato di caricamento
- Redirect al success page in caso di successo

**Funzionalità principali:**
- `nextStep()` / `previousStep()`: Navigazione tra i step con validazione
- `submitForm()`: Invio del form al backend
- `getFieldError()`: Estrazione dei messaggi di errore validazione
- `isFieldInvalid()`: Verifica se un campo è invalido e tocco
- Progress tracking visuale

#### `BusinessSignupComponent.html`
Template HTML con:
- Header informativo
- Progress bar con step indicators (1/2/3)
- Tre form sections nascoste dinamicamente
- Alert messages per errori e successi
- Form groups responsive per desktop e mobile
- Validazione real-time con feedback visuale
- Pulsanti di navigazione (Indietro/Avanti/Registrati)

#### `BusinessSignupComponent.scss`
Styling completo con:
- Gradient background (viola/blu)
- Tema colore coerente (primary, success, error)
- Layout responsive (mobile, tablet, desktop)
- Animazioni di transizione tra step
- Componenti form stilizzati (input, select, button)
- Progress bar animata
- Alert box con stile appropriato
- Media queries per breakpoint mobile (480px), tablet (768px), desktop (1024px)

### 3. Componente di Successo

#### `SignupSuccessComponent.ts`
- Pagina di successo post-registrazione
- Countdown automatico di 10 secondi
- Redirect al login dopo il countdown
- Pulsante per reindirizzamento manuale

#### `SignupSuccessComponent.html`
Template con:
- Icona di successo (checkmark)
- Messaggio di conferma
- Lista dei prossimi passi
- Countdown timer
- Pulsante di reindirizzamento al login

#### `SignupSuccessComponent.scss`
Styling per la pagina di successo con animazioni.

### 4. Routing

#### `public.routes.ts`
Definisce le rotte pubbliche:
- `/public/signup` → BusinessSignupComponent
- `/public/signup-success` → SignupSuccessComponent

#### `app.routes.ts` (Aggiornato)
Integra le rotte pubbliche nel routing principale:
```typescript
{ path: 'public', children: PUBLIC_ROUTES }
```

## Configurazione dell'Ambiente

### `environment.ts`
File di configurazione con:
```typescript
apiUrl: 'http://localhost:8080'
```

Deve essere aggiornato per gli ambienti di produzione.

## Flusso Completo di Registrazione

### Lato Client (Angular)
1. Utente accede a `/public/signup`
2. Compila il form multi-step (3 steps)
3. Validazione in tempo reale sui campi
4. Al submit, invia POST a `/api/public/business-registration/signup`
5. Se successo: redirect a `/public/signup-success`
6. Se errore: mostra messaggio di errore

### Lato Server (Spring Boot)
1. Riceve richiesta POST con BusinessSignupRequest
2. Valida i dati (password match, slug uniqueness, email unique, ecc.)
3. Crea il tenant in status PENDING
4. Crea lo staff_user (contatto primario)
5. Assegna il ruolo MANAGER
6. Crea la subscription del tenant
7. Crea la business_registration_request
8. Ritorna BusinessSignupResponse con tenantId e status
9. Tutto in una transazione atomica

## Stato Iniziale dei Nuovi Tenant

Dopo la registrazione, un tenant ha:
- **Status:** PENDING (non operazionale)
- **Staff User:** Creato e ACTIVE (può essere usato per login in futuro)
- **Subscription:** Status PENDING (non attivo)
- **Registration Request:** Status CONVERTED (traccia storica)

## Prossimi Step per Completare l'Implementazione

### Backend
1. **Configurare Spring Security:** Proteggere l'endpoint di approvazione `/approve`
2. **Implementare Email Service:** Inviare email di conferma al contatto primario
3. **Aggiungere Cache:** Redis per validazioni di slug uniqueness
4. **Logging e Monitoring:** Structured logging per audit trail
5. **Error Handling Centralizzato:** GlobalExceptionHandler per CodesCustom
6. **Password Encoder:** Assicurarsi che BCryptPasswordEncoder sia configurato

### Frontend
1. **Implementare Login:** Form di login per gli staff user registrati
2. **Aggiungere Spinner:** Loading indicators durante le operazioni
3. **Validazione Live del Slug:** Controllare disponibilità in tempo reale
4. **Integrazione con Tenant Context:** Gestire il tenant corrente dopo login
5. **Redirect Intelligente:** Verso dashboard appropriato post-login
6. **Traduzioni i18n:** Supportare multiple lingue

### Database
1. **Eseguire Flyway Migration:** Il schema è già creato in schema.sql
2. **Seed Data:** Popolare i staff_roles (MANAGER, BAR, KITCHEN, RUNNER)

## Testing

### Backend (JUnit + Mockito)
```java
// Test cases suggeriti
- testSubmitValidRegistration()
- testDuplicateSlugValidation()
- testPasswordMismatchValidation()
- testDuplicateEmailValidation()
- testTransactionalRollback()
- testApproveTenant()
```

### Frontend (Jasmine + Karma)
```typescript
// Test cases suggeriti
- testFormInitialization()
- testStepNavigation()
- testFormValidation()
- testSubmitSuccess()
- testSubmitError()
- testCountdownTimer()
```

## Struttura File Creati

```
Backend:
src/main/java/com/orderapp/ordering/
├── dto/
│   ├── BusinessSignupRequest.java
│   └── BusinessSignupResponse.java
├── entity/
│   ├── Tenant.java (aggiornata)
│   ├── BusinessRegistrationRequest.java
│   ├── StaffUser.java
│   ├── StaffRole.java
│   ├── StaffUserRole.java
│   └── TenantSubscription.java
├── repository/
│   ├── BusinessRegistrationRequestRepository.java
│   ├── TenantRepository.java
│   ├── StaffUserRepository.java
│   ├── StaffUserRoleRepository.java
│   ├── StaffRoleRepository.java
│   └── TenantSubscriptionRepository.java
├── service/
│   └── BusinessRegistrationService.java
└── controller/
    └── BusinessRegistrationController.java

Frontend:
src/app/features/public/
├── services/
│   └── business-registration.service.ts
├── pages/
│   ├── business-signup/
│   │   ├── business-signup.component.ts
│   │   ├── business-signup.component.html
│   │   └── business-signup.component.scss
│   └── signup-success/
│       ├── signup-success.component.ts
│       ├── signup-success.component.html
│       └── signup-success.component.scss
└── public.routes.ts

Config:
src/environments/
└── environment.ts

Root Routing:
src/app/
└── app.routes.ts (aggiornata)
```

## URL di Accesso

Sviluppo locale:
- Frontend: `http://localhost:4200/public/signup`
- Backend API: `http://localhost:8080/api/public/business-registration/signup`

## Note Importanti

1. **Transazionalità:** Il servizio utilizza `@Transactional` per garantire atomicità
2. **Isolamento Tenant:** Tutte le entità sono associate al tenant_id
3. **Password Security:** Utilizza BCryptPasswordEncoder (da configurare in SecurityConfig)
4. **Validazione Bean:** DTOs hanno validazioni complete con messaggi in italiano
5. **CORS:** Controller ha @CrossOrigin per permettere richieste dal frontend
6. **Error Handling:** Messaggi di errore informativi in italiano
7. **Responsive Design:** Frontend responsive per tutti i device

## Integrazione con Registrazione.md

L'implementazione segue completamente i requisiti di registrazione.md:
- ✅ Creazione tenant in status PENDING
- ✅ Creazione staff_user con is_primary_contact=true
- ✅ Assegnazione ruolo MANAGER
- ✅ Creazione subscription
- ✅ Creazione business_registration_request
- ✅ Validazioni complete
- ✅ Transazione atomica
- ✅ DTO con bean validation
- ✅ Controller REST pubblico
- ✅ Service layer ben organizzato
- ✅ Repository pattern
- ✅ Nessun auto-activate del tenant
- ✅ Nessuna session JWT creata
