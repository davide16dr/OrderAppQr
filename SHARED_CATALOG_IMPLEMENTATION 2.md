# Implementazione Catalogo Condiviso - Aree e Categorie

## Panoramica
Questo documento documenta la migrazione da un modello tenant-specific a un modello di **catalogo condiviso** per aree di postazioni e categorie di prodotti.

## Cambio Architetturale

### Prima: Tenant-Specific
```
Tenant 1 → Area "Terrazza" (id: 1)
Tenant 2 → Area "Terrazza" (id: 2)  ❌ Duplicato
```

### Ora: Catalogo Condiviso
```
Area "Terrazza" (id: 1) [Globale]
├── Tenant 1 → tenant_areas(1, 1)
└── Tenant 2 → tenant_areas(2, 1)  ✅ Riutilizzo stesso record
```

## Struttura Database

### Tabelle Modificate
1. **areas** - Rimosso `tenant_id`, aggiunto UNIQUE su `name` (case-insensitive logic in app)
2. **categories** - Rimosso `tenant_id`, aggiunto UNIQUE su `name`

### Tabelle Nuove
1. **tenant_areas** - Join table con PK (tenant_id, area_id)
   - `status` - ACTIVE/DISABLED per disconnessione tenant
   - `created_at`, `updated_at`

2. **tenant_categories** - Join table con PK (tenant_id, category_id)
   - `status` - ACTIVE/DISABLED per disconnessione tenant
   - `created_at`, `updated_at`

## Comportamento del Sistema

### Creare Area/Categoria
```
1. Controlla se esiste nel DB (LOWER(name) = LOWER(?))
   ✓ Esiste → Riutilizza
   ✗ Non esiste → Crea nuova
2. Crea collegamento in tenant_areas/tenant_categories
3. Controlla se tenant già ha accesso → Errore "già assegnato"
```

### Modificare Area/Categoria
- Solo il tenant che l'ha creata può modificarla (verificato via JOIN)
- Modifiche si riflettono su **tutti i tenant** che usano quella risorsa

### Eliminare Area/Categoria
```
1. Elimina il collegamento in tenant_areas/tenant_categories
2. La risorsa globale rimane nel DB
   - Disponibile per altri tenant
   - Conserva storico ordini/prodotti
```

## Entity Java

### AreaEntity
```java
@Entity
@Table(name = "areas")
public class AreaEntity {
    Long id;
    String name;         // UNIQUE
    String description;
    Integer displayOrder;
    String status;       // ACTIVE, DISABLED
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
```

### TenantAreaEntity
```java
@Entity
@Table(name = "tenant_areas")
public class TenantAreaEntity {
    TenantAreaId id;     // (tenant_id, area_id)
    Tenant tenant;
    AreaEntity area;
    String status;       // ACTIVE, DISABLED
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
```

Stesso per `CategoryEntity` e `TenantCategoryEntity`.

## Repository

### Nuovi Repository JPA
- `AreaJpaRepository` - Query sull'area globale
- `TenantAreaJpaRepository` - Query sul join table
- `CategoryJpaRepository` - Query sulla categoria globale
- `TenantCategoryJpaRepository` - Query sul join table

### Queries Principali
```java
// Ottenere aree attive di un tenant
findActiveTenantAreas(Long tenantId)

// Verificare se una risorsa esiste già nel DB
findByNameIgnoreCase(String name)

// Disconnettere un tenant
deleteByIdTenantIdAndIdAreaId(Long tenantId, Long areaId)
```

## Migrazione Dati

Il file `V2__SharedCatalogPattern.sql` esegue:
1. Consolida duplicati case-insensitive → mantiene primo per created_at
2. Crea `tenant_areas` e `tenant_categories` da dati vecchi
3. Aggiorna `locations` e `category_tenant_products` per referenziare record consolidati
4. Rimuove duplicate
5. Rimuove colonna `tenant_id` da `areas` e `categories`

## API - Comportamento Finale

### ✅ Same Endpoints
```
GET    /api/categories           → Ritorna categorie globali del tenant
POST   /api/categories           → Crea/Connette categoria
PUT    /api/categories/{id}      → Modifica categoria globale
DELETE /api/categories/{id}      → Disconnette categoria dal tenant

GET    /api/areas                → Ritorna aree globali del tenant
POST   /api/areas                → Crea/Connette area
PUT    /api/areas/{id}           → Modifica area globale
DELETE /api/areas/{id}           → Disconnette area dal tenant
```

### DTO Response (No Changes)
```json
{
  "id": 1,
  "name": "Terrazza",
  "description": "Area principale",
  "displayOrder": 0,
  "status": "ACTIVE"
}
```

## Vantaggi

1. **Zero Duplicati** - Stessa area/categoria riutilizzata tra tenant
2. **Economico DB** - Meno spazio per aree/categorie
3. **Consistent** - Modifica a area globale si riflette su tutti
4. **Soft-Disconnect** - Niente perdita dati, solo disconnessione
5. **Backward Compatible** - Stesso DTO, stesso endpoint

## Migrazione Database Locale

Per rigenerare il DB locale con il nuovo schema:
```bash
# Elimina il DB (se usando H2/file-based)
rm target/h2_database.db

# O usa Flyway
mvn flyway:migrate
```

## Prossimi Passi Consigliati

1. ✅ Testare compilazione backend
2. Test integrazione con nuovo schema
3. Verificare comportamento creazione/modifica/eliminazione
4. Update frontend se necessario (probabilmente no - stesso DTO)
5. Rollout con migrazione Flyway
