# Admin Overview Dashboard — Design Spec
**Datum:** 2026-03-20
**Status:** Approved

## Zusammenfassung

Ein neuer Admin-Screen unter `/admin/overview` bietet einen vollständigen Überblick über die Applikation: KPI-Kacheln für User-Zahlen und Feature-Nutzung sowie ein vollständiges, persistentes Audit-Log das sowohl Admin- als auch User-Aktionen trackt.

---

## 1. Datenmodell

### Tabelle: `audit_logs`

| Feld | Typ | Beschreibung |
|---|---|---|
| `id` | BIGINT PK AUTO_INCREMENT | Primärschlüssel |
| `timestamp` | DATETIME NOT NULL | Zeitpunkt des Ereignisses |
| `actor_id` | BIGINT NULL (FK → users) | Auslöser (null = System) |
| `actor_username` | VARCHAR(100) NULL | Denormalisiert für historische Korrektheit |
| `action` | VARCHAR(100) NOT NULL | Enum-Wert der Aktion |
| `target_type` | VARCHAR(50) NULL | Typ des betroffenen Objekts |
| `target_id` | VARCHAR(50) NULL | ID des betroffenen Objekts |
| `details` | TEXT NULL | Optionaler JSON-String mit Zusatzinfos |

**Wichtig:** `actor_username` wird zum Zeitpunkt des Events denormalisiert gespeichert, damit historische Log-Einträge korrekt bleiben, auch wenn ein Username später geändert wird.

### Enum: `AuditAction`

```java
public enum AuditAction {
    // Auth
    LOGIN, PASSWORD_CHANGED, EMAIL_VERIFIED,
    // Admin
    USER_CREATED, USER_UPDATED, USER_STATUS_CHANGED, SUBSCRIPTION_CHANGED,
    // User
    FIT_UPLOADED, STRAVA_CONNECTED, STRAVA_DISCONNECTED,
    TRAINING_PLAN_ASSIGNED, PROFILE_UPDATED,
    // System
    PLAN_CREATED, PLAN_DELETED, COMPETITION_CREATED, COMPETITION_DELETED
}
```

**Hinweis:** `LOGOUT` wurde aus dem Enum entfernt, da kein Logout-Endpoint im System existiert. Kann später ergänzt werden wenn ein Logout-Mechanismus implementiert wird.

---

## 2. Backend-Architektur

### Spring-Konfiguration (Pflicht-Ergänzungen)

Beide folgenden Annotationen müssen zu `SecurityConfig` (oder einer dedizierten `AsyncConfig`-Klasse) hinzugefügt werden, **bevor** der Feature-Branch gemergt wird:

- `@EnableAsync` — ohne diese Annotation führt `@Async` auf `AuditLogService.log()` nichts aus; die Methode läuft synchron ohne Fehler oder Warnung
- `@EnableMethodSecurity(prePostEnabled = true)` — ohne diese Annotation werden `@PreAuthorize`-Annotationen ignoriert und Admin-Endpoints sind für alle authentifizierten User erreichbar

### Neue Klassen

| Klasse | Paket | Beschreibung |
|---|---|---|
| `AuditLog` | `entity` | JPA-Entity für `audit_logs` |
| `AuditAction` | `entity` | Enum aller Aktionen |
| `AuditLogRepository` | `repository` | Spring Data JPA, mit Page-Support |
| `AuditLogService` | `service` | Zentraler Log-Service, `@Async` Schreibmethode |
| `AdminStatsService` | `service` | KPI-Aggregation per JPQL |
| `AdminStatsDto` | `dto` | KPI-Response |
| `AuditLogDto` | `dto` | Einzelner Log-Eintrag für API-Response |
| `AdminController` | `controller` | `/api/admin/*`, gesichert mit `@PreAuthorize("hasRole('ADMIN')")` |

### API-Endpoints

```
GET /api/admin/stats
→ AdminStatsDto

GET /api/admin/audit-log?page=0&size=50&action=LOGIN&from=2026-01-01&to=2026-03-20
→ Page<AuditLogDto>
```

### AdminStatsDto — Felder

```json
{
  "totalUsers": 42,
  "activeUsers": 38,
  "inactiveUsers": 2,
  "blockedUsers": 1,
  "pendingVerification": 1,
  "newUsersThisWeek": 5,
  "newUsersThisMonth": 12,
  "stravaConnected": 15,
  "asthmaTrackingEnabled": 8,
  "cycleTrackingEnabled": 11,
  "paceZonesConfigured": 20
}
```

**KPI-Definitionen:**
- `activeUsers` — `UserStatus.ACTIVE`
- `inactiveUsers` — `UserStatus.INACTIVE`
- `blockedUsers` — `UserStatus.BLOCKED`
- `pendingVerification` — `UserStatus.EMAIL_VERIFICATION_PENDING` + `UserStatus.ADMIN_APPROVAL_PENDING` (bewusst aggregiert für v1; beide Status erfordern Admin-Aufmerksamkeit, aber unterschiedliche Aktionen — kann später in zwei Felder aufgeteilt werden)
- `paceZonesConfigured` — Anzahl User wo `thresholdPaceSecPerKm IS NOT NULL`

### AuditLogDto — Felder

```json
{
  "id": 123,
  "timestamp": "2026-03-20T14:32:10",
  "actorId": 5,
  "actorUsername": "admin",
  "action": "USER_UPDATED",
  "targetType": "USER",
  "targetId": "12",
  "details": "{\"changedFields\": [\"status\", \"subscriptionPlan\"]}"
}
```

`details` wird als roher JSON-String zurückgegeben. Das Frontend rendert ihn als formatierten Text in einem Tooltip oder Dialog.

### AuditLogService

- Methode: `log(User actor, AuditAction action, String targetType, String targetId, Map<String, Object> details)`
- Annotiert mit `@Async` — läuft in einem separaten Thread-Pool-Thread
- `actor` darf null sein (System-Events) — dann bleiben `actor_id` und `actor_username` null
- `details` wird per `ObjectMapper` als JSON-String serialisiert

**Wichtig:** Der Async-Thread erbt den `SecurityContext` NICHT (Spring's Default ist `MODE_THREADLOCAL`). `AuditLogService.log()` darf deshalb nie `SecurityContextHolder.getContext()` aufrufen. Der Actor wird immer explizit als Parameter übergeben — nie aus dem Security-Context gelesen.

### Integration in bestehende Services

Explizite `auditLogService.log(...)` Aufrufe (kein AOP), damit klar ist was getrackt wird:

| Service / Controller | Methode | Action |
|---|---|---|
| `AuthController` | `login()` | `LOGIN` |
| `UserService` | `createUser()` | `USER_CREATED` |
| `UserService` | `updateUser()` | `USER_UPDATED` |
| `UserService` | `updateUser()` wenn Status sich ändert | `USER_STATUS_CHANGED` |
| `UserService` | `updateUser()` wenn Subscription sich ändert | `SUBSCRIPTION_CHANGED` |
| `CompletedTrainingService` | Upload-Methode | `FIT_UPLOADED` |
| `StravaService` | connect / disconnect | `STRAVA_CONNECTED` / `STRAVA_DISCONNECTED` |
| `TrainingPlanService` | create / delete | `PLAN_CREATED` / `PLAN_DELETED` |
| `CompetitionService` | create / delete | `COMPETITION_CREATED` / `COMPETITION_DELETED` |

### Liquibase-Migration

Neue Datei: `changes/065-add-audit-logs.xml`
Mit `<preConditions onFail="MARK_RAN">` wie im Projekt-Standard. Include in `db.changelog-master.xml` anfügen.

---

## 3. Frontend-Architektur

### Sicherheit: Admin-Route Guard (Pflicht)

Der bestehende `authGuard` prüft nur ob ein User eingeloggt ist, nicht ob er die ADMIN-Rolle hat. Es muss ein `adminGuard` erstellt werden, der die ADMIN-Rolle prüft und bei Fehler auf `/` umleitet.

**Implementierung der Rollenprüfung:** Die `AuthResponse` beim Login enthält bereits ein `role`-Feld. `AuthService.login()` speichert die Rolle zusammen mit dem JWT-Token in `localStorage` (Key: `role`). Der `adminGuard` liest `localStorage.getItem('role') === 'ADMIN'`. Dies ist konsistent mit dem bestehenden Muster (Token wird ebenfalls in `localStorage` gespeichert).

Der Guard wird auf die `/admin`-Route in `app.routes.ts` angewendet (nicht nur auf `/admin/overview`, sondern auf den gesamten Admin-Bereich).

### Neue Route

`/admin/overview` → Komponente `AdminOverview`

Die Admin-Shell leitet ab jetzt standardmäßig auf `/admin/overview` statt auf `/admin/plans` weiter.

Ein neuer Tab `Overview` mit Icon `dashboard` wird als erster Tab in `admin-shell.html` eingefügt.

### Komponente: `AdminOverview`

**Datei-Struktur:**
```
frontend/src/app/components/admin/overview/
  admin-overview.ts
  admin-overview.html
  admin-overview.scss
```

**Layout:**

```
┌─────────────────────────────────────────────────────┐
│  KPI-GRUPPE: User-Zahlen                            │
│  [Gesamt] [Aktiv] [Inaktiv] [Gesperrt] [Ausstehend] [Neu/Woche] │
├─────────────────────────────────────────────────────┤
│  KPI-GRUPPE: Feature-Nutzung                        │
│  [Strava] [Asthma-Tracking] [Cycle] [Pace-Zonen]   │
├─────────────────────────────────────────────────────┤
│  AUDIT-LOG                                          │
│  Filter: [Action-Kategorie ▼] [Von] [Bis] [Suchen] │
│  ┌──────────┬────────┬──────────┬────────┬────────┐ │
│  │Timestamp │ Actor  │ Action   │ Target │Details │ │
│  ├──────────┼────────┼──────────┼────────┼────────┤ │
│  │...       │...     │...       │...     │...     │ │
│  └──────────┴────────┴──────────┴────────┴────────┘ │
│  [Pagination]                                       │
└─────────────────────────────────────────────────────┘
```

**Angular Material Komponenten:**
- KPI-Kacheln: `mat-card` mit Icon + Zahl
- Tabelle: `mat-table` + `mat-paginator`
- Filter: `mat-select` (Kategorie), `mat-date-range-input` (Zeitraum)
- Details: `mat-icon-button` mit Tooltip oder Dialog für JSON-Details

### TypeScript-Interfaces

```typescript
interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  blockedUsers: number;
  pendingVerification: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  stravaConnected: number;
  asthmaTrackingEnabled: number;
  cycleTrackingEnabled: number;
  paceZonesConfigured: number;
}

interface AuditLogEntry {
  id: number;
  timestamp: string;
  actorId: number | null;
  actorUsername: string | null;
  action: string;
  targetType: string | null;
  targetId: string | null;
  details: string | null;
}
```

### Neuer Service: `AdminService`

```typescript
// frontend/src/app/services/admin.service.ts
// Folgt der bestehenden Konvention: hardcodierte Base-URL wie alle anderen Services
const BASE = 'http://localhost:8080/api/admin';

getStats(): Observable<AdminStats>
getAuditLog(params: { page: number; size: number; action?: string; from?: string; to?: string }): Observable<{ content: AuditLogEntry[]; totalElements: number }>
```

---

## 4. Sicherheit — Zusammenfassung

| Ebene | Maßnahme |
|---|---|
| Spring (Backend) | `@EnableMethodSecurity(prePostEnabled = true)` in `SecurityConfig` |
| Endpoint | `@PreAuthorize("hasRole('ADMIN')")` auf `AdminController` |
| Async | `@EnableAsync` in `SecurityConfig` (oder `AsyncConfig`) |
| Frontend Route | Neuer `adminGuard` prüft ADMIN-Rolle, Redirect auf `/` bei Fehler |

---

## 5. Nicht in diesem Scope

- Audit-Log-Export (CSV/Excel) — kann später ergänzt werden
- Real-time Updates via WebSocket — kann später ergänzt werden
- Audit-Log für Lese-Aktionen (GET-Requests) — zu viel Rauschen, nur schreibende Aktionen werden getrackt
- `LOGOUT`-Action — kein Logout-Endpoint vorhanden, kann später ergänzt werden
