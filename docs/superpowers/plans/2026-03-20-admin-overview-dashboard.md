# Admin Overview Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Einen `/admin/overview` Screen mit KPI-Kacheln (User-Zahlen, Feature-Nutzung) und einem persistenten Audit-Log (Admin- und User-Aktionen) implementieren.

**Architecture:** Neuer `AuditLogService` mit `@Async`-Schreibmethode schreibt Events aus bestehenden Services. Neuer `AdminController` (`/api/admin/*`) stellt Stats und paginiertes Log bereit. Angular-Komponente `AdminOverview` zeigt KPI-Cards + gefilterte Log-Tabelle.

**Tech Stack:** Spring Boot 3.2, JPA/H2 (Tests), Liquibase, Angular 19 standalone, TypeScript signals

---

## File Map

### Backend — Neue Dateien
- `backend/src/main/java/com/trainingsplan/entity/AuditAction.java` — Enum aller Audit-Aktionen
- `backend/src/main/java/com/trainingsplan/entity/AuditLog.java` — JPA-Entity
- `backend/src/main/java/com/trainingsplan/repository/AuditLogRepository.java` — Spring Data JPA mit Page-Support
- `backend/src/main/java/com/trainingsplan/service/AuditLogService.java` — @Async Log-Methode
- `backend/src/main/java/com/trainingsplan/service/AdminStatsService.java` — JPQL KPI-Aggregation
- `backend/src/main/java/com/trainingsplan/dto/AdminStatsDto.java` — KPI-Response-Record
- `backend/src/main/java/com/trainingsplan/dto/AuditLogDto.java` — Log-Eintrag-Response-Record
- `backend/src/main/java/com/trainingsplan/controller/AdminController.java` — REST-Endpoints
- `backend/src/main/resources/db/changelog/changes/065-add-audit-logs.xml` — Liquibase-Migration
- `backend/src/test/java/com/trainingsplan/service/AdminStatsServiceTest.java`
- `backend/src/test/java/com/trainingsplan/service/AuditLogServiceTest.java`

### Backend — Modifizierte Dateien
- `backend/src/main/java/com/trainingsplan/config/SecurityConfig.java` — `@EnableAsync` + `@EnableMethodSecurity` hinzufügen
- `backend/src/main/resources/db/changelog/db.changelog-master.xml` — Include für 065 anfügen
- `backend/src/main/java/com/trainingsplan/controller/AuthController.java` — `LOGIN` Audit-Call
- `backend/src/main/java/com/trainingsplan/service/UserService.java` — `USER_CREATED`, `USER_UPDATED`, `USER_STATUS_CHANGED`, `SUBSCRIPTION_CHANGED`
- `backend/src/main/java/com/trainingsplan/service/CompletedTrainingService.java` — `FIT_UPLOADED`
- `backend/src/main/java/com/trainingsplan/service/StravaService.java` — `STRAVA_CONNECTED`, `STRAVA_DISCONNECTED`
- `backend/src/main/java/com/trainingsplan/service/TrainingPlanService.java` — `PLAN_CREATED`, `PLAN_DELETED`
- `backend/src/main/java/com/trainingsplan/service/CompetitionService.java` — `COMPETITION_CREATED`, `COMPETITION_DELETED`

### Frontend — Neue Dateien
- `frontend/src/app/guards/admin.guard.ts` — Rolle-Guard (ADMIN)
- `frontend/src/app/services/admin.service.ts` — `getStats()` + `getAuditLog()`
- `frontend/src/app/components/admin/overview/admin-overview.ts`
- `frontend/src/app/components/admin/overview/admin-overview.html`
- `frontend/src/app/components/admin/overview/admin-overview.scss`

### Frontend — Modifizierte Dateien
- `frontend/src/app/services/auth.service.ts` — Role in localStorage speichern/löschen
- `frontend/src/app/app.routes.ts` — `adminGuard` für `/admin`-Route hinzufügen
- `frontend/src/app/components/admin/admin.routes.ts` — `/overview` Route, Default-Redirect
- `frontend/src/app/components/admin/admin-shell/admin-shell.html` — Overview-Tab ergänzen

---

## Task 1: Spring Config — @EnableAsync + @EnableMethodSecurity

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/config/SecurityConfig.java`

- [ ] **Step 1: Annotationen zur SecurityConfig hinzufügen**

Öffne `SecurityConfig.java`. Die Klasse beginnt mit:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
```

Ändere auf:
```java
@Configuration
@EnableWebSecurity
@EnableAsync
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
```

Ergänze die Imports:
```java
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
```

- [ ] **Step 2: Backend kompilieren und prüfen**

```bash
cd backend
mvn compile -q
```
Erwartetes Ergebnis: BUILD SUCCESS, keine Fehler.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/config/SecurityConfig.java
git commit -m "feat: enable @Async and @EnableMethodSecurity in SecurityConfig"
```

---

## Task 2: Liquibase-Migration — audit_logs Tabelle

**Files:**
- Create: `backend/src/main/resources/db/changelog/changes/065-add-audit-logs.xml`
- Modify: `backend/src/main/resources/db/changelog/db.changelog-master.xml`

- [ ] **Step 1: Migration-Datei erstellen**

Erstelle `backend/src/main/resources/db/changelog/changes/065-add-audit-logs.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="065-add-audit-logs" author="system">
        <preConditions onFail="MARK_RAN">
            <not><tableExists tableName="audit_logs"/></not>
        </preConditions>

        <createTable tableName="audit_logs">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="timestamp" type="DATETIME">
                <constraints nullable="false"/>
            </column>
            <column name="actor_id" type="BIGINT"/>
            <column name="actor_username" type="VARCHAR(100)"/>
            <column name="action" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="target_type" type="VARCHAR(50)"/>
            <column name="target_id" type="VARCHAR(50)"/>
            <column name="details" type="TEXT"/>
        </createTable>

        <createIndex tableName="audit_logs" indexName="idx_audit_logs_timestamp">
            <column name="timestamp"/>
        </createIndex>

        <createIndex tableName="audit_logs" indexName="idx_audit_logs_actor_id">
            <column name="actor_id"/>
        </createIndex>

        <createIndex tableName="audit_logs" indexName="idx_audit_logs_action">
            <column name="action"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Include in Master-Changelog anfügen**

In `db.changelog-master.xml`, nach der Zeile `<include file="db/changelog/changes/064-add-user-last-login.xml"/>` einfügen:

```xml
    <include file="db/changelog/changes/065-add-audit-logs.xml"/>
```

- [ ] **Step 3: Backend starten und Migration prüfen**

```bash
cd backend
mvn spring-boot:run
```
Prüfe die Log-Ausgabe auf: `Running Changeset: 065-add-audit-logs.xml::065-add-audit-logs`. Dann mit `Ctrl+C` stoppen.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/changelog/changes/065-add-audit-logs.xml
git add backend/src/main/resources/db/changelog/db.changelog-master.xml
git commit -m "feat: add audit_logs table migration (065)"
```

---

## Task 3: AuditAction Enum + AuditLog Entity + AuditLogRepository

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/entity/AuditAction.java`
- Create: `backend/src/main/java/com/trainingsplan/entity/AuditLog.java`
- Create: `backend/src/main/java/com/trainingsplan/repository/AuditLogRepository.java`

- [ ] **Step 1: AuditAction Enum erstellen**

```java
package com.trainingsplan.entity;

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

- [ ] **Step 2: AuditLog Entity erstellen**

```java
package com.trainingsplan.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", length = 50)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    public AuditLog() {}

    public AuditLog(LocalDateTime timestamp, Long actorId, String actorUsername,
                    AuditAction action, String targetType, String targetId, String details) {
        this.timestamp = timestamp;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    // Getters
    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Long getActorId() { return actorId; }
    public String getActorUsername() { return actorUsername; }
    public AuditAction getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getDetails() { return details; }
}
```

- [ ] **Step 3: AuditLogRepository erstellen**

```java
package com.trainingsplan.repository;

import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:action IS NULL OR a.action = :action)
          AND (:from IS NULL OR a.timestamp >= :from)
          AND (:to IS NULL OR a.timestamp <= :to)
        ORDER BY a.timestamp DESC
        """)
    Page<AuditLog> findFiltered(
            @Param("action") AuditAction action,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
```

- [ ] **Step 4: Kompilieren**

```bash
cd backend && mvn compile -q
```
Erwartetes Ergebnis: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/entity/AuditAction.java
git add backend/src/main/java/com/trainingsplan/entity/AuditLog.java
git add backend/src/main/java/com/trainingsplan/repository/AuditLogRepository.java
git commit -m "feat: add AuditAction enum, AuditLog entity and AuditLogRepository"
```

---

## Task 4: AuditLogService

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/service/AuditLogService.java`
- Create: `backend/src/test/java/com/trainingsplan/service/AuditLogServiceTest.java`

- [ ] **Step 1: Test schreiben**

```java
package com.trainingsplan.service;

import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.AuditLog;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogRepository repository;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        service = new AuditLogService(repository);
    }

    @Test
    void log_withActor_savesCorrectFields() {
        User actor = new User("testuser", "test@example.com", LocalDateTime.now());
        // Setze ID via Reflection, da kein Setter vorhanden
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(actor, 42L);
        } catch (Exception e) { throw new RuntimeException(e); }

        service.log(actor, AuditAction.LOGIN, null, null, null);

        var captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(42L, saved.getActorId());
        assertEquals("testuser", saved.getActorUsername());
        assertEquals(AuditAction.LOGIN, saved.getAction());
        assertNull(saved.getTargetType());
    }

    @Test
    void log_withoutActor_savesNullActorFields() {
        service.log(null, AuditAction.PLAN_CREATED, "TRAINING_PLAN", "5", null);

        var captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertNull(saved.getActorId());
        assertNull(saved.getActorUsername());
        assertEquals("TRAINING_PLAN", saved.getTargetType());
        assertEquals("5", saved.getTargetId());
    }

    @Test
    void log_withDetails_serialisesAsJson() {
        service.log(null, AuditAction.USER_UPDATED, "USER", "1",
                Map.of("changedFields", "status"));

        var captor = forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getDetails());
        assertTrue(captor.getValue().getDetails().contains("changedFields"));
    }
}
```

- [ ] **Step 2: Test fehlschlagen lassen**

```bash
cd backend && mvn test -Dtest=AuditLogServiceTest -q
```
Erwartetes Ergebnis: FAIL (Klasse existiert noch nicht).

- [ ] **Step 3: AuditLogService implementieren**

```java
package com.trainingsplan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.entity.AuditLog;
import com.trainingsplan.entity.User;
import com.trainingsplan.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Schreibt einen Audit-Log-Eintrag asynchron in einer eigenen Transaktion.
     * WICHTIG: Darf NICHT SecurityContextHolder.getContext() aufrufen — der Async-Thread
     * erbt den SecurityContext nicht. Actor wird immer explizit übergeben.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor, AuditAction action, String targetType, String targetId,
                    Map<String, Object> details) {
        try {
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            AuditLog entry = new AuditLog(
                    LocalDateTime.now(),
                    actor != null ? actor.getId() : null,
                    actor != null ? actor.getUsername() : null,
                    action,
                    targetType,
                    targetId,
                    detailsJson
            );
            repository.save(entry);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log details", e);
        }
    }
}
```

Ergänze in pom.xml Mockito, falls nicht vorhanden (prüfe zuerst):
```bash
grep -n "mockito" backend/pom.xml
```
Falls nicht vorhanden, in `<dependencies>` einfügen:
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 4: Tests laufen lassen**

```bash
cd backend && mvn test -Dtest=AuditLogServiceTest -q
```
Erwartetes Ergebnis: BUILD SUCCESS, 3 Tests passed.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/AuditLogService.java
git add backend/src/test/java/com/trainingsplan/service/AuditLogServiceTest.java
git commit -m "feat: add AuditLogService with @Async log method"
```

---

## Task 5: AdminStatsDto + AuditLogDto + AdminStatsService

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/dto/AdminStatsDto.java`
- Create: `backend/src/main/java/com/trainingsplan/dto/AuditLogDto.java`
- Create: `backend/src/main/java/com/trainingsplan/service/AdminStatsService.java`
- Create: `backend/src/test/java/com/trainingsplan/service/AdminStatsServiceTest.java`

- [ ] **Step 1: DTOs erstellen**

`AdminStatsDto.java`:
```java
package com.trainingsplan.dto;

public record AdminStatsDto(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long blockedUsers,
        long pendingVerification,
        long newUsersThisWeek,
        long newUsersThisMonth,
        long stravaConnected,
        long asthmaTrackingEnabled,
        long cycleTrackingEnabled,
        long paceZonesConfigured
) {}
```

`AuditLogDto.java`:
```java
package com.trainingsplan.dto;

import com.trainingsplan.entity.AuditLog;
import java.time.LocalDateTime;

public record AuditLogDto(
        Long id,
        LocalDateTime timestamp,
        Long actorId,
        String actorUsername,
        String action,
        String targetType,
        String targetId,
        String details
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getTimestamp(),
                log.getActorId(),
                log.getActorUsername(),
                log.getAction().name(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails()
        );
    }
}
```

- [ ] **Step 2: Test für AdminStatsService schreiben**

```java
package com.trainingsplan.service;

import com.trainingsplan.dto.AdminStatsDto;
import com.trainingsplan.entity.User;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AdminStatsServiceTest {

    private UserRepository userRepository;
    private AdminStatsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AdminStatsService(userRepository);
    }

    @Test
    void getStats_countsUsersCorrectly() {
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(5L);
        when(userRepository.countByStatus(UserStatus.INACTIVE)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.BLOCKED)).thenReturn(2L);
        when(userRepository.countByStatusIn(anyList())).thenReturn(3L);
        when(userRepository.count()).thenReturn(11L);
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(4L, 8L);
        when(userRepository.countByStravaTokenIsNotNull()).thenReturn(6L);
        when(userRepository.countByAsthmaTrackingEnabledTrue()).thenReturn(7L);
        when(userRepository.countByCycleTrackingEnabledTrue()).thenReturn(9L);
        when(userRepository.countByThresholdPaceSecPerKmIsNotNull()).thenReturn(10L);

        AdminStatsDto stats = service.getStats();

        assertEquals(11L, stats.totalUsers());
        assertEquals(5L, stats.activeUsers());
        assertEquals(1L, stats.inactiveUsers());
        assertEquals(2L, stats.blockedUsers());
        assertEquals(3L, stats.pendingVerification());
    }
}
```

- [ ] **Step 3: Test fehlschlagen lassen**

```bash
cd backend && mvn test -Dtest=AdminStatsServiceTest -q
```
Erwartetes Ergebnis: FAIL.

- [ ] **Step 4: UserRepository um Count-Methoden erweitern**

Öffne `backend/src/main/java/com/trainingsplan/repository/UserRepository.java`. Ergänze folgende Methoden (alle werden von Spring Data automatisch implementiert):

```java
import com.trainingsplan.entity.UserStatus;
import java.time.LocalDateTime;
import java.util.List;

long countByStatus(UserStatus status);
long countByStatusIn(List<UserStatus> statuses);
long countByCreatedAtAfter(LocalDateTime date);
long countByStravaTokenIsNotNull();
long countByAsthmaTrackingEnabledTrue();
long countByCycleTrackingEnabledTrue();
long countByThresholdPaceSecPerKmIsNotNull();
```

- [ ] **Step 5: AdminStatsService implementieren**

```java
package com.trainingsplan.service;

import com.trainingsplan.dto.AdminStatsDto;
import com.trainingsplan.entity.UserStatus;
import com.trainingsplan.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;

    public AdminStatsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AdminStatsDto getStats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        return new AdminStatsDto(
                userRepository.count(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.INACTIVE),
                userRepository.countByStatus(UserStatus.BLOCKED),
                userRepository.countByStatusIn(List.of(
                        UserStatus.EMAIL_VERIFICATION_PENDING,
                        UserStatus.ADMIN_APPROVAL_PENDING)),
                userRepository.countByCreatedAtAfter(weekAgo),
                userRepository.countByCreatedAtAfter(monthAgo),
                userRepository.countByStravaTokenIsNotNull(),
                userRepository.countByAsthmaTrackingEnabledTrue(),
                userRepository.countByCycleTrackingEnabledTrue(),
                userRepository.countByThresholdPaceSecPerKmIsNotNull()
        );
    }
}
```

- [ ] **Step 6: Tests laufen lassen**

```bash
cd backend && mvn test -Dtest=AdminStatsServiceTest -q
```
Erwartetes Ergebnis: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/dto/AdminStatsDto.java
git add backend/src/main/java/com/trainingsplan/dto/AuditLogDto.java
git add backend/src/main/java/com/trainingsplan/service/AdminStatsService.java
git add backend/src/main/java/com/trainingsplan/repository/UserRepository.java
git add backend/src/test/java/com/trainingsplan/service/AdminStatsServiceTest.java
git commit -m "feat: add AdminStatsService, AdminStatsDto and AuditLogDto"
```

---

## Task 6: AdminController

**Files:**
- Create: `backend/src/main/java/com/trainingsplan/controller/AdminController.java`

- [ ] **Step 1: AdminController erstellen**

```java
package com.trainingsplan.controller;

import com.trainingsplan.dto.AdminStatsDto;
import com.trainingsplan.dto.AuditLogDto;
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.repository.AuditLogRepository;
import com.trainingsplan.service.AdminStatsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final AuditLogRepository auditLogRepository;

    public AdminController(AdminStatsService adminStatsService,
                           AuditLogRepository auditLogRepository) {
        this.adminStatsService = adminStatsService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLogDto>> getAuditLog(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        AuditAction actionEnum = action != null && !action.isBlank()
                ? AuditAction.valueOf(action) : null;
        LocalDateTime fromDt = from != null ? LocalDate.parse(from).atStartOfDay() : null;
        LocalDateTime toDt = to != null ? LocalDate.parse(to).atTime(23, 59, 59) : null;

        Page<AuditLogDto> result = auditLogRepository
                .findFiltered(actionEnum, fromDt, toDt, PageRequest.of(page, size))
                .map(AuditLogDto::from);

        return ResponseEntity.ok(result);
    }
}
```

- [ ] **Step 2: Kompilieren und alle Tests laufen lassen**

```bash
cd backend && mvn test -q
```
Erwartetes Ergebnis: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/controller/AdminController.java
git commit -m "feat: add AdminController with /api/admin/stats and /api/admin/audit-log"
```

---

## Task 7: Audit-Integration in AuthController (LOGIN)

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/controller/AuthController.java`

- [ ] **Step 1: AuditLogService in AuthController injizieren und LOGIN loggen**

Füge `AuditLogService` als Konstruktor-Parameter hinzu und rufe `log()` nach erfolgreichem Login auf.

In `AuthController`:
```java
// Import hinzufügen:
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.service.AuditLogService;

// Feld hinzufügen:
private final AuditLogService auditLogService;

// Konstruktor erweitern:
public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      JwtService jwtService, AuthenticationManager authenticationManager,
                      EmailService emailService, AuditLogService auditLogService) {
    // ... bestehende Zuweisungen ...
    this.auditLogService = auditLogService;
}
```

In der `login()`-Methode, NACH `userRepository.save(authenticatedUser)`:
```java
auditLogService.log(authenticatedUser, AuditAction.LOGIN, null, null, null);
```

- [ ] **Step 2: Kompilieren**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/controller/AuthController.java
git commit -m "feat: log LOGIN events via AuditLogService"
```

---

## Task 8: Audit-Integration in UserService

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/UserService.java`

- [ ] **Step 1: AuditLogService in UserService injizieren**

Feld und Konstruktor in `UserService` erweitern:
```java
// Import:
import com.trainingsplan.entity.AuditAction;
import com.trainingsplan.service.AuditLogService;

private final AuditLogService auditLogService;

public UserService(UserRepository userRepository, SecurityUtils securityUtils,
                   ImageStoragePort imageStoragePort, AuditLogService auditLogService) {
    this.userRepository = userRepository;
    this.securityUtils = securityUtils;
    this.imageStoragePort = imageStoragePort;
    this.auditLogService = auditLogService;
}
```

- [ ] **Step 2: USER_CREATED in createUser() loggen**

In `createUser()`, nach `userRepository.save(user)`:
```java
auditLogService.log(null, AuditAction.USER_CREATED, "USER",
        String.valueOf(user.getId()), null);
```

- [ ] **Step 3: USER_UPDATED, USER_STATUS_CHANGED, SUBSCRIPTION_CHANGED in updateUser() loggen**

In `updateUser()`, BEVOR `userRepository.save(user)`, speichere alte Werte:
```java
UserStatus oldStatus = user.getStatus();
SubscriptionPlan oldPlan = user.getSubscriptionPlan();
```

Nach `userRepository.save(user)`:
```java
User caller = securityUtils.getCurrentUser();
auditLogService.log(caller, AuditAction.USER_UPDATED, "USER", String.valueOf(id), null);

if (status != null && !status.isBlank() && !UserStatus.valueOf(status).equals(oldStatus)) {
    auditLogService.log(caller, AuditAction.USER_STATUS_CHANGED, "USER", String.valueOf(id),
            Map.of("from", oldStatus.name(), "to", status));
}
if (subscriptionPlan != null && !subscriptionPlan.isBlank()
        && !SubscriptionPlan.valueOf(subscriptionPlan).equals(oldPlan)) {
    auditLogService.log(caller, AuditAction.SUBSCRIPTION_CHANGED, "USER", String.valueOf(id),
            Map.of("from", oldPlan.name(), "to", subscriptionPlan));
}
```

Import ergänzen: `import java.util.Map;`

- [ ] **Step 4: Kompilieren**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/UserService.java
git commit -m "feat: log USER_CREATED, USER_UPDATED, USER_STATUS_CHANGED, SUBSCRIPTION_CHANGED"
```

---

## Task 9: Audit-Integration in CompletedTrainingService, StravaService, TrainingPlanService, CompetitionService

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/CompletedTrainingService.java`
- Modify: `backend/src/main/java/com/trainingsplan/service/StravaService.java`
- Modify: `backend/src/main/java/com/trainingsplan/service/TrainingPlanService.java`
- Modify: `backend/src/main/java/com/trainingsplan/service/CompetitionService.java`

Für jeden Service gilt dasselbe Muster:
1. `AuditLogService` als Konstruktor-Parameter hinzufügen
2. An der relevanten Stelle `auditLogService.log(...)` aufrufen

- [ ] **Step 1: CompletedTrainingService — FIT_UPLOADED**

Finde die Upload-/Save-Methode in `CompletedTrainingService`. Nach erfolgreichem Speichern:
```java
auditLogService.log(currentUser, AuditAction.FIT_UPLOADED, "COMPLETED_TRAINING",
        String.valueOf(saved.getId()), null);
```

- [ ] **Step 2: StravaService — STRAVA_CONNECTED und STRAVA_DISCONNECTED**

Finde die connect- und disconnect-Methoden. Nach erfolgreicher Verbindung bzw. Trennung:
```java
// connect:
auditLogService.log(user, AuditAction.STRAVA_CONNECTED, "USER", String.valueOf(user.getId()), null);
// disconnect:
auditLogService.log(user, AuditAction.STRAVA_DISCONNECTED, "USER", String.valueOf(user.getId()), null);
```

- [ ] **Step 3: TrainingPlanService — PLAN_CREATED und PLAN_DELETED**

Nach erfolgreichem Erstellen/Löschen:
```java
// create:
auditLogService.log(currentUser, AuditAction.PLAN_CREATED, "TRAINING_PLAN",
        String.valueOf(plan.getId()), null);
// delete:
auditLogService.log(currentUser, AuditAction.PLAN_DELETED, "TRAINING_PLAN",
        String.valueOf(planId), null);
```

- [ ] **Step 4: CompetitionService — COMPETITION_CREATED und COMPETITION_DELETED**

```java
// create:
auditLogService.log(currentUser, AuditAction.COMPETITION_CREATED, "COMPETITION",
        String.valueOf(competition.getId()), null);
// delete:
auditLogService.log(currentUser, AuditAction.COMPETITION_DELETED, "COMPETITION",
        String.valueOf(competitionId), null);
```

- [ ] **Step 5: Alle Tests laufen lassen**

```bash
cd backend && mvn test -q
```
Erwartetes Ergebnis: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/
git commit -m "feat: integrate audit logging into CompletedTraining, Strava, TrainingPlan, Competition services"
```

---

## Task 10: Frontend — AuthService Rolle in localStorage

**Files:**
- Modify: `frontend/src/app/services/auth.service.ts`

- [ ] **Step 1: ROLE_KEY Konstante + Rolle speichern/löschen**

In `auth.service.ts`:

```typescript
const TOKEN_KEY = 'auth_token';
const ROLE_KEY = 'auth_role';   // NEU
```

In der `login()`-Methode, im `tap`:
```typescript
tap(res => {
  localStorage.setItem(TOKEN_KEY, res.token);
  localStorage.setItem(ROLE_KEY, res.role);  // NEU
  this._isLoggedIn.set(true);
})
```

In der `logout()`-Methode:
```typescript
logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY);  // NEU
  this._isLoggedIn.set(false);
}
```

Neue Methode hinzufügen:
```typescript
getRole(): string | null {
  return localStorage.getItem(ROLE_KEY);
}
```

- [ ] **Step 2: Frontend kompilieren**

```bash
cd frontend && npm run build -- --no-progress 2>&1 | tail -5
```
Erwartetes Ergebnis: Application bundle generation complete.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/services/auth.service.ts
git commit -m "feat: store and clear user role in localStorage on login/logout"
```

---

## Task 11: Frontend — adminGuard

**Files:**
- Create: `frontend/src/app/guards/admin.guard.ts`
- Modify: `frontend/src/app/app.routes.ts`

- [ ] **Step 1: adminGuard erstellen**

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn() && auth.getRole() === 'ADMIN') {
    return true;
  }

  return router.createUrlTree(['/']);
};
```

- [ ] **Step 2: adminGuard in app.routes.ts ergänzen**

Die `/admin`-Route in `app.routes.ts` von:
```typescript
{
  path: 'admin',
  canActivate: [authGuard],
  loadChildren: () => import('./components/admin/admin.routes').then(m => m.adminRoutes)
}
```
Auf:
```typescript
{
  path: 'admin',
  canActivate: [authGuard, adminGuard],
  loadChildren: () => import('./components/admin/admin.routes').then(m => m.adminRoutes)
}
```

Import hinzufügen: `import { adminGuard } from './guards/admin.guard';`

- [ ] **Step 3: Kompilieren**

```bash
cd frontend && npm run build -- --no-progress 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/guards/admin.guard.ts
git add frontend/src/app/app.routes.ts
git commit -m "feat: add adminGuard for role-based route protection"
```

---

## Task 12: Frontend — AdminService

**Files:**
- Create: `frontend/src/app/services/admin.service.ts`

- [ ] **Step 1: AdminService erstellen**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = 'http://localhost:8080/api/admin';

export interface AdminStats {
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

export interface AuditLogEntry {
  id: number;
  timestamp: string;
  actorId: number | null;
  actorUsername: string | null;
  action: string;
  targetType: string | null;
  targetId: string | null;
  details: string | null;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${BASE}/stats`);
  }

  getAuditLog(params: {
    page: number;
    size: number;
    action?: string;
    from?: string;
    to?: string;
  }): Observable<AuditLogPage> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size);
    if (params.action) httpParams = httpParams.set('action', params.action);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);

    return this.http.get<AuditLogPage>(`${BASE}/audit-log`, { params: httpParams });
  }
}
```

- [ ] **Step 2: Kompilieren**

```bash
cd frontend && npm run build -- --no-progress 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/services/admin.service.ts
git commit -m "feat: add AdminService with getStats() and getAuditLog()"
```

---

## Task 13: Frontend — AdminOverview Komponente

**Files:**
- Create: `frontend/src/app/components/admin/overview/admin-overview.ts`
- Create: `frontend/src/app/components/admin/overview/admin-overview.html`
- Create: `frontend/src/app/components/admin/overview/admin-overview.scss`

- [ ] **Step 1: admin-overview.ts erstellen**

```typescript
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, AdminStats, AuditLogEntry } from '../../../services/admin.service';

@Component({
  selector: 'app-admin-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './admin-overview.html',
  styleUrl: './admin-overview.scss'
})
export class AdminOverview implements OnInit {
  private adminService = inject(AdminService);

  stats = signal<AdminStats | null>(null);
  auditLog = signal<AuditLogEntry[]>([]);
  totalElements = signal(0);
  totalPages = signal(0);
  isLoadingStats = signal(false);
  isLoadingLog = signal(false);
  hasError = signal(false);

  // Filter
  filterAction = '';
  filterFrom = '';
  filterTo = '';
  currentPage = 0;
  pageSize = 50;

  readonly actionCategories = [
    { label: 'Alle', value: '' },
    { label: 'Login', value: 'LOGIN' },
    { label: 'User erstellt', value: 'USER_CREATED' },
    { label: 'User bearbeitet', value: 'USER_UPDATED' },
    { label: 'Status geändert', value: 'USER_STATUS_CHANGED' },
    { label: 'Subscription geändert', value: 'SUBSCRIPTION_CHANGED' },
    { label: 'FIT Upload', value: 'FIT_UPLOADED' },
    { label: 'Strava verbunden', value: 'STRAVA_CONNECTED' },
    { label: 'Strava getrennt', value: 'STRAVA_DISCONNECTED' },
    { label: 'Plan erstellt', value: 'PLAN_CREATED' },
    { label: 'Plan gelöscht', value: 'PLAN_DELETED' },
    { label: 'Competition erstellt', value: 'COMPETITION_CREATED' },
    { label: 'Competition gelöscht', value: 'COMPETITION_DELETED' },
  ];

  ngOnInit(): void {
    this.loadStats();
    this.loadAuditLog();
  }

  loadStats(): void {
    this.isLoadingStats.set(true);
    this.adminService.getStats().subscribe({
      next: (data) => { this.stats.set(data); this.isLoadingStats.set(false); },
      error: () => { this.hasError.set(true); this.isLoadingStats.set(false); }
    });
  }

  loadAuditLog(): void {
    this.isLoadingLog.set(true);
    this.adminService.getAuditLog({
      page: this.currentPage,
      size: this.pageSize,
      action: this.filterAction || undefined,
      from: this.filterFrom || undefined,
      to: this.filterTo || undefined
    }).subscribe({
      next: (data) => {
        this.auditLog.set(data.content);
        this.totalElements.set(data.totalElements);
        this.totalPages.set(data.totalPages);
        this.isLoadingLog.set(false);
      },
      error: () => { this.hasError.set(true); this.isLoadingLog.set(false); }
    });
  }

  applyFilter(): void {
    this.currentPage = 0;
    this.loadAuditLog();
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.loadAuditLog();
  }

  formatDetails(details: string | null): string {
    if (!details) return '—';
    try {
      return JSON.stringify(JSON.parse(details), null, 2);
    } catch {
      return details;
    }
  }
}
```

- [ ] **Step 2: admin-overview.html erstellen**

```html
<div class="overview-page">

  <section class="kpi-section">
    <h2 class="section-title">User-Zahlen</h2>
    <div class="kpi-grid" *ngIf="stats()">
      <div class="kpi-card">
        <span class="kpi-label">Gesamt</span>
        <span class="kpi-value">{{ stats()!.totalUsers }}</span>
      </div>
      <div class="kpi-card kpi-green">
        <span class="kpi-label">Aktiv</span>
        <span class="kpi-value">{{ stats()!.activeUsers }}</span>
      </div>
      <div class="kpi-card kpi-yellow">
        <span class="kpi-label">Inaktiv</span>
        <span class="kpi-value">{{ stats()!.inactiveUsers }}</span>
      </div>
      <div class="kpi-card kpi-red">
        <span class="kpi-label">Gesperrt</span>
        <span class="kpi-value">{{ stats()!.blockedUsers }}</span>
      </div>
      <div class="kpi-card kpi-orange">
        <span class="kpi-label">Ausstehend</span>
        <span class="kpi-value">{{ stats()!.pendingVerification }}</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">Neu (7 Tage)</span>
        <span class="kpi-value">{{ stats()!.newUsersThisWeek }}</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">Neu (30 Tage)</span>
        <span class="kpi-value">{{ stats()!.newUsersThisMonth }}</span>
      </div>
    </div>
  </section>

  <section class="kpi-section">
    <h2 class="section-title">Feature-Nutzung</h2>
    <div class="kpi-grid" *ngIf="stats()">
      <div class="kpi-card">
        <span class="kpi-label">Strava verbunden</span>
        <span class="kpi-value">{{ stats()!.stravaConnected }}</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">Asthma-Tracking</span>
        <span class="kpi-value">{{ stats()!.asthmaTrackingEnabled }}</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">Cycle-Tracking</span>
        <span class="kpi-value">{{ stats()!.cycleTrackingEnabled }}</span>
      </div>
      <div class="kpi-card">
        <span class="kpi-label">Pace-Zonen</span>
        <span class="kpi-value">{{ stats()!.paceZonesConfigured }}</span>
      </div>
    </div>
  </section>

  <section class="audit-section">
    <h2 class="section-title">Audit-Log</h2>

    <div class="filter-bar">
      <select [(ngModel)]="filterAction" class="filter-select">
        <option *ngFor="let cat of actionCategories" [value]="cat.value">{{ cat.label }}</option>
      </select>
      <input type="date" [(ngModel)]="filterFrom" class="filter-date" placeholder="Von">
      <input type="date" [(ngModel)]="filterTo" class="filter-date" placeholder="Bis">
      <button (click)="applyFilter()" class="filter-btn">Filtern</button>
    </div>

    <div class="log-table-wrapper">
      <table class="log-table" *ngIf="!isLoadingLog(); else loadingTpl">
        <thead>
          <tr>
            <th>Zeitpunkt</th>
            <th>Actor</th>
            <th>Aktion</th>
            <th>Ziel</th>
            <th>Details</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let entry of auditLog()">
            <td class="cell-timestamp">{{ entry.timestamp | date:'dd.MM.yy HH:mm:ss' }}</td>
            <td>{{ entry.actorUsername ?? '—' }}</td>
            <td><span class="action-badge">{{ entry.action }}</span></td>
            <td>{{ entry.targetType ? entry.targetType + ' #' + entry.targetId : '—' }}</td>
            <td>
              <span class="details-cell" *ngIf="entry.details" [title]="formatDetails(entry.details)">
                &#128269;
              </span>
              <span *ngIf="!entry.details">—</span>
            </td>
          </tr>
          <tr *ngIf="auditLog().length === 0">
            <td colspan="5" class="empty-row">Keine Einträge gefunden.</td>
          </tr>
        </tbody>
      </table>
      <ng-template #loadingTpl><p class="loading-hint">Lade...</p></ng-template>
    </div>

    <div class="pagination" *ngIf="totalPages() > 1">
      <button (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 0">‹ Zurück</button>
      <span>Seite {{ currentPage + 1 }} von {{ totalPages() }} ({{ totalElements() }} Einträge)</span>
      <button (click)="goToPage(currentPage + 1)" [disabled]="currentPage >= totalPages() - 1">Weiter ›</button>
    </div>
  </section>

</div>
```

- [ ] **Step 3: admin-overview.scss erstellen**

```scss
.overview-page {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #666;
  margin: 0 0 12px;
}

.kpi-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.kpi-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.kpi-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px 20px;
  min-width: 120px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: 1px solid #e9ecef;

  &.kpi-green { border-left: 3px solid #28a745; }
  &.kpi-yellow { border-left: 3px solid #ffc107; }
  &.kpi-red { border-left: 3px solid #dc3545; }
  &.kpi-orange { border-left: 3px solid #fd7e14; }
}

.kpi-label {
  font-size: 12px;
  color: #888;
}

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: #212529;
}

.audit-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-select, .filter-date {
  padding: 7px 10px;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
}

.filter-btn {
  padding: 7px 16px;
  background: #0d6efd;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;

  &:hover { background: #0b5ed7; }
}

.log-table-wrapper {
  overflow-x: auto;
}

.log-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th {
    text-align: left;
    padding: 10px 12px;
    background: #f8f9fa;
    border-bottom: 2px solid #dee2e6;
    font-weight: 600;
    color: #495057;
  }

  td {
    padding: 9px 12px;
    border-bottom: 1px solid #f0f0f0;
    vertical-align: middle;
  }

  tr:hover td { background: #f8f9fa; }
}

.cell-timestamp {
  white-space: nowrap;
  color: #666;
  font-family: monospace;
  font-size: 12px;
}

.action-badge {
  background: #e9ecef;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.03em;
  font-family: monospace;
}

.details-cell {
  cursor: pointer;
  font-size: 16px;
}

.empty-row {
  text-align: center;
  color: #999;
  padding: 24px !important;
}

.loading-hint {
  color: #999;
  padding: 16px 0;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  font-size: 14px;
  color: #555;

  button {
    padding: 6px 12px;
    border: 1px solid #dee2e6;
    background: #fff;
    border-radius: 4px;
    cursor: pointer;

    &:disabled { opacity: 0.4; cursor: default; }
    &:not(:disabled):hover { background: #f8f9fa; }
  }
}
```

- [ ] **Step 4: Kompilieren**

```bash
cd frontend && npm run build -- --no-progress 2>&1 | tail -5
```
Erwartetes Ergebnis: Application bundle generation complete.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/components/admin/overview/
git commit -m "feat: add AdminOverview component with KPI tiles and audit log table"
```

---

## Task 14: Frontend — Routing und Navigation aktualisieren

**Files:**
- Modify: `frontend/src/app/components/admin/admin.routes.ts`
- Modify: `frontend/src/app/components/admin/admin-shell/admin-shell.html`

- [ ] **Step 1: admin.routes.ts aktualisieren**

Die Route für `/overview` ergänzen und Default-Redirect ändern.

In `admin.routes.ts`:
```typescript
import { Routes } from '@angular/router';
import { AdminShell } from './admin-shell/admin-shell';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminShell,
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },  // GEÄNDERT
      {
        path: 'overview',  // NEU
        loadComponent: () => import('./overview/admin-overview').then(m => m.AdminOverview)
      },
      // ... bestehende Routen unverändert ...
    ]
  }
];
```

- [ ] **Step 2: Overview-Tab in admin-shell.html ergänzen**

Als ersten `<a>`-Tag in `<nav class="admin-tabs">` einfügen:
```html
<a routerLink="overview" routerLinkActive="active" class="admin-tab">
  <span class="material-symbols-outlined">dashboard</span>
  <span>Overview</span>
</a>
```

- [ ] **Step 3: Kompilieren und auf Fehler prüfen**

```bash
cd frontend && npm run build -- --no-progress 2>&1 | tail -10
```
Erwartetes Ergebnis: Application bundle generation complete, keine Fehler.

- [ ] **Step 4: Manuell testen**

```bash
cd frontend && npm start
```
Navigiere zu `http://localhost:4200/admin` und prüfe:
- Redirect geht zu `/admin/overview`
- Overview-Tab erscheint als erster Tab
- KPI-Kacheln laden (ggf. mit Testdaten)
- Audit-Log-Tabelle zeigt bestehende Einträge oder "Keine Einträge gefunden"

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/components/admin/admin.routes.ts
git add frontend/src/app/components/admin/admin-shell/admin-shell.html
git commit -m "feat: add /admin/overview route and Overview navigation tab"
```

---

## Abschluss

- [ ] **Alle Backend-Tests laufen lassen**

```bash
cd backend && mvn test -q
```
Erwartetes Ergebnis: BUILD SUCCESS.

- [ ] **Frontend-Build final prüfen**

```bash
cd frontend && npm run build -- --no-progress 2>&1 | tail -5
```
Erwartetes Ergebnis: Application bundle generation complete.

- [ ] **End-to-End manuell testen**

1. Backend starten: `cd backend && mvn spring-boot:run`
2. Frontend starten: `cd frontend && npm start`
3. Als Admin einloggen → `/admin/overview` prüfen
4. Einen anderen User bearbeiten → Audit-Log prüfen ob USER_UPDATED erscheint
5. Als Normal-User einloggen → `/admin` aufrufen → prüfen ob Redirect auf `/` erfolgt
