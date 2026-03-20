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
| `actor_username` | VARCHAR(100) | Denormalisiert für historische Korrektheit |
| `action` | VARCHAR(100) NOT NULL | Enum-Wert der Aktion |
| `target_type` | VARCHAR(50) NULL | Typ des betroffenen Objekts |
| `target_id` | VARCHAR(50) NULL | ID des betroffenen Objekts |
| `details` | TEXT NULL | Optionaler JSON-String mit Zusatzinfos |

**Wichtig:** `actor_username` wird zum Zeitpunkt des Events denormalisiert gespeichert, damit historische Log-Einträge korrekt bleiben, auch wenn ein Username später geändert wird.

### Enum: `AuditAction`

```java
public enum AuditAction {
    // Auth
    LOGIN, LOGOUT, PASSWORD_CHANGED, EMAIL_VERIFIED,
    // Admin
    USER_CREATED, USER_UPDATED, USER_STATUS_CHANGED, SUBSCRIPTION_CHANGED,
    // User
    FIT_UPLOADED, STRAVA_CONNECTED, STRAVA_DISCONNECTED,
    TRAINING_PLAN_ASSIGNED, PROFILE_UPDATED,
    // System
    PLAN_CREATED, PLAN_DELETED, COMPETITION_CREATED, COMPETITION_DELETED
}
```

---

## 2. Backend-Architektur

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
  "inactiveUsers": 3,
  "blockedUsers": 1,
  "newUsersThisWeek": 5,
  "newUsersThisMonth": 12,
  "stravaConnected": 15,
  "asthmaTrackingEnabled": 8,
  "cycleTrackingEnabled": 11,
  "paceZonesConfigured": 20
}
```

### AuditLogService

- Methode: `log(User actor, AuditAction action, String targetType, String targetId, Map<String, Object> details)`
- Läuft mit `@Async` um die Performance der auslösenden Aktion nicht zu beeinträchtigen
- `actor` darf null sein (System-Events)
- `details` wird als JSON-String serialisiert und in `details`-Feld gespeichert

### Integration in bestehende Services

Explizite `auditLogService.log(...)` Aufrufe (kein AOP), damit klar ist was getrackt wird:

| Service | Methode | Action |
|---|---|---|
| `AuthService` / Login-Filter | `login()` | `LOGIN` |
| `UserService` | `createUser()` | `USER_CREATED` |
| `UserService` | `updateUser()` | `USER_UPDATED` |
| `UserService` | `updateUser()` (Status-Änderung) | `USER_STATUS_CHANGED` |
| `UserService` | `updateUser()` (Subscription-Änderung) | `SUBSCRIPTION_CHANGED` |
| `CompletedTrainingService` | `upload()` | `FIT_UPLOADED` |
| `StravaService` | `connect()` / `disconnect()` | `STRAVA_CONNECTED` / `STRAVA_DISCONNECTED` |
| `TrainingPlanService` | `create()` / `delete()` | `PLAN_CREATED` / `PLAN_DELETED` |
| `CompetitionService` | `create()` / `delete()` | `COMPETITION_CREATED` / `COMPETITION_DELETED` |

### Liquibase-Migration

Neue Datei: `changes/064-add-audit-logs.xml`
Mit `<preConditions onFail="MARK_RAN">` wie im Projekt-Standard.

---

## 3. Frontend-Architektur

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
│  [Gesamt] [Aktiv] [Inaktiv/Gesperrt] [Neu/Woche]   │
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

### Neuer Service: `AdminService`

```typescript
// frontend/src/app/services/admin.service.ts
getStats(): Observable<AdminStats>
getAuditLog(params): Observable<Page<AuditLogEntry>>
```

---

## 4. Sicherheit

- Alle `/api/admin/*` Endpoints sind mit `@PreAuthorize("hasRole('ADMIN')")` gesichert
- Frontend: Die `/admin/overview` Route ist bereits durch den bestehenden Admin-Guard geschützt (sofern vorhanden), sonst wird ein Route-Guard ergänzt

---

## 5. Nicht in diesem Scope

- Audit-Log-Export (CSV/Excel) — kann später ergänzt werden
- Real-time Updates via WebSocket — kann später ergänzt werden
- Audit-Log für Lese-Aktionen (GET-Requests) — zu viel Rauschen, nur schreibende Aktionen werden getrackt
