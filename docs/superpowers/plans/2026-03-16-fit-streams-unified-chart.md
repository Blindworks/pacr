# FIT Streams Unified Chart — Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Capture all per-second data streams from FIT files (HR, altitude, speed/pace, cadence, power, GPS) and display them in a single unified SVG chart with multiple Y-axes and a toggleable legend in the activity detail view.

**Architecture:** Extend the existing `FitDataCollector` inner class to collect 7 additional parallel stream lists from FIT record messages, persist them into the existing `ActivityStream` entity after upload, extend `ActivityStreamDto` with cadence/power fields, and replace the dual-overlay HR/elevation chart in `activity-detail` with a unified multi-stream chart.

**Tech Stack:** Java 21, Spring Boot 3.2, Garmin FIT SDK, Jackson ObjectMapper, Liquibase, Angular 19, TypeScript Signals, SVG

**Spec:** `docs/superpowers/specs/2026-03-16-fit-streams-unified-chart-design.md`

---

## Chunk 1: Backend

### Task 1: Liquibase Migration 054

**Files:**
- Create: `backend/src/main/resources/db/changelog/changes/054-add-activity-stream-cadence-power.xml`
- Modify: `backend/src/main/resources/db/changelog/db.changelog-master.xml`

- [ ] **Step 1.1: Create migration file**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <changeSet id="054-add-activity-stream-cadence-json" author="system">
        <preConditions onFail="MARK_RAN">
            <not><columnExists tableName="activity_streams" columnName="cadence_json"/></not>
        </preConditions>
        <addColumn tableName="activity_streams">
            <column name="cadence_json" type="LONGTEXT"/>
        </addColumn>
    </changeSet>

    <changeSet id="054-add-activity-stream-power-json" author="system">
        <preConditions onFail="MARK_RAN">
            <not><columnExists tableName="activity_streams" columnName="power_json"/></not>
        </preConditions>
        <addColumn tableName="activity_streams">
            <column name="power_json" type="LONGTEXT"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 1.2: Register in master changelog**

In `db.changelog-master.xml`, add after the line containing `053-extend-body-metrics-for-daily.xml`:

```xml
    <include file="db/changelog/changes/054-add-activity-stream-cadence-power.xml"/>
```

- [ ] **Step 1.3: Verify migration runs**

```bash
cd backend && mvn spring-boot:run
```

Expected: no Liquibase errors in startup log. Check log for `054-add-activity-stream-cadence-json: ran` and `054-add-activity-stream-power-json: ran`.

- [ ] **Step 1.4: Commit**

```bash
git add backend/src/main/resources/db/changelog/changes/054-add-activity-stream-cadence-power.xml
git add backend/src/main/resources/db/changelog/db.changelog-master.xml
git commit -m "feat: add cadence_json and power_json columns to activity_streams (migration 054)"
```

---

### Task 2: ActivityStream Entity — New Columns

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/entity/ActivityStream.java`

- [ ] **Step 2.1: Add fields and getters/setters**

After the existing `latlngJson` field (line 50), add:

```java
/** Serialized Integer[] of cadence values in spm/rpm (may contain nulls). */
@Column(name = "cadence_json", columnDefinition = "LONGTEXT")
private String cadenceJson;

/** Serialized Integer[] of power values in watts (may contain nulls). */
@Column(name = "power_json", columnDefinition = "LONGTEXT")
private String powerJson;
```

After the existing `setFetchedAt` getter/setter, add:

```java
public String getCadenceJson() { return cadenceJson; }
public void setCadenceJson(String cadenceJson) { this.cadenceJson = cadenceJson; }

public String getPowerJson() { return powerJson; }
public void setPowerJson(String powerJson) { this.powerJson = powerJson; }
```

- [ ] **Step 2.2: Compile check**

```bash
cd backend && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2.3: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/entity/ActivityStream.java
git commit -m "feat: add cadenceJson and powerJson to ActivityStream entity"
```

---

### Task 3: ActivityStreamDto + ActivityStreamService

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/dto/ActivityStreamDto.java`
- Modify: `backend/src/main/java/com/trainingsplan/service/ActivityStreamService.java`

- [ ] **Step 3.1: Extend ActivityStreamDto record**

Replace the existing record in `ActivityStreamDto.java`:

```java
package com.trainingsplan.dto;

/**
 * Downsampled stream data for a completed training activity, ready for charting.
 * Arrays are aligned by index — each index represents one sample point.
 *
 * @param completedTrainingId the source activity ID
 * @param distancePoints      distance in km at each sample (x-axis for all charts)
 * @param heartRate           heart rate in bpm per sample; null entries indicate HR dropouts
 * @param altitude            altitude in meters per sample; null when stream unavailable
 * @param paceSecondsPerKm    pace in seconds/km per sample (1000 / velocity_m_s); null for stops
 * @param cadence             cadence in spm/rpm per sample; null entries where data absent
 * @param power               power in watts per sample; null entries where data absent
 * @param hasHeartRate        true when HR stream contains at least one non-null value
 * @param hasAltitude         true when altitude stream contains at least one non-null value
 * @param hasPace             true when velocity stream contains at least one non-null value
 * @param hasCadence          true when cadence stream contains at least one non-null value
 * @param hasPower            true when power stream contains at least one non-null value
 */
public record ActivityStreamDto(
        Long completedTrainingId,
        double[] distancePoints,
        Integer[] heartRate,
        Double[] altitude,
        Integer[] paceSecondsPerKm,
        Integer[] cadence,
        Integer[] power,
        boolean hasHeartRate,
        boolean hasAltitude,
        boolean hasPace,
        boolean hasCadence,
        boolean hasPower
) {}
```

- [ ] **Step 3.2: Update ActivityStreamService**

Replace the full `getStreamDto()` method and add the `anyNonNull` helper. The key changes are:
1. Parse `cadenceJson` and `powerJson` like the existing streams
2. Allocate `cadence[]` and `power[]` arrays in the sampling loop
3. Replace all 3 `has*` string-presence checks with `anyNonNull` on the post-loop typed arrays
4. Update the DTO constructor call

Replace the body of `ActivityStreamService.java` with:

```java
package com.trainingsplan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.dto.ActivityStreamDto;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.repository.ActivityStreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ActivityStreamService {

    private static final Logger log = LoggerFactory.getLogger(ActivityStreamService.class);
    private static final int MAX_POINTS = 200;

    private final ActivityStreamRepository activityStreamRepository;
    private final ObjectMapper objectMapper;

    public ActivityStreamService(ActivityStreamRepository activityStreamRepository,
                                 ObjectMapper objectMapper) {
        this.activityStreamRepository = activityStreamRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<ActivityStreamDto> getStreamDto(Long completedTrainingId) {
        Optional<ActivityStream> streamOpt = activityStreamRepository.findByCompletedTrainingId(completedTrainingId);
        if (streamOpt.isEmpty()) return Optional.empty();

        ActivityStream stream = streamOpt.get();
        try {
            List<Double> rawDistances = parseDoubleList(stream.getDistanceJson());
            if (rawDistances == null || rawDistances.isEmpty()) return Optional.empty();

            int n = rawDistances.size();
            int step = Math.max(1, n / MAX_POINTS);
            int sampledCount = (n + step - 1) / step;

            List<Integer> rawHeartRates  = parseIntegerList(stream.getHeartrateJson());
            List<Double>  rawAltitudes   = parseDoubleList(stream.getAltitudeJson());
            List<Double>  rawVelocities  = parseDoubleList(stream.getVelocitySmoothJson());
            List<Integer> rawCadences    = parseIntegerList(stream.getCadenceJson());
            List<Integer> rawPowers      = parseIntegerList(stream.getPowerJson());

            double[]  distancePoints    = new double[sampledCount];
            Integer[] heartRate         = rawHeartRates  != null ? new Integer[sampledCount] : null;
            Double[]  altitude          = rawAltitudes   != null ? new Double[sampledCount]  : null;
            Integer[] paceSecondsPerKm  = rawVelocities  != null ? new Integer[sampledCount] : null;
            Integer[] cadence           = rawCadences    != null ? new Integer[sampledCount] : null;
            Integer[] power             = rawPowers      != null ? new Integer[sampledCount] : null;

            int idx = 0;
            for (int i = 0; i < n; i += step) {
                distancePoints[idx] = rawDistances.get(i) / 1000.0;

                if (rawHeartRates != null && i < rawHeartRates.size())
                    heartRate[idx] = rawHeartRates.get(i);
                if (rawAltitudes != null && i < rawAltitudes.size())
                    altitude[idx] = rawAltitudes.get(i);
                if (rawVelocities != null && i < rawVelocities.size()) {
                    Double v = rawVelocities.get(i);
                    paceSecondsPerKm[idx] = (v != null && v > 0.1) ? (int)(1000.0 / v) : null;
                }
                if (rawCadences != null && i < rawCadences.size())
                    cadence[idx] = rawCadences.get(i);
                if (rawPowers != null && i < rawPowers.size())
                    power[idx] = rawPowers.get(i);

                idx++;
            }

            return Optional.of(new ActivityStreamDto(
                    completedTrainingId,
                    distancePoints,
                    heartRate,
                    altitude,
                    paceSecondsPerKm,
                    cadence,
                    power,
                    anyNonNull(heartRate),
                    anyNonNull(altitude),
                    anyNonNull(paceSecondsPerKm),
                    anyNonNull(cadence),
                    anyNonNull(power)
            ));
        } catch (Exception e) {
            log.error("Failed to parse stream data for completedTrainingId={}: {}", completedTrainingId, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean anyNonNull(Object[] arr) {
        return arr != null && Arrays.stream(arr).anyMatch(Objects::nonNull);
    }

    private List<Double> parseDoubleList(String json) throws Exception {
        if (json == null || json.isBlank()) return null;
        return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
    }

    private List<Integer> parseIntegerList(String json) throws Exception {
        if (json == null || json.isBlank()) return null;
        return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
    }
}
```

- [ ] **Step 3.3: Compile check**

```bash
cd backend && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3.4: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/dto/ActivityStreamDto.java
git add backend/src/main/java/com/trainingsplan/service/ActivityStreamService.java
git commit -m "feat: add cadence and power streams to ActivityStreamDto and ActivityStreamService"
```

---

### Task 4: FitDataCollector — Extended Stream Collection

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/CompletedTrainingService.java`

#### Step 4.1 — Add 7 new raw lists to the `FitDataCollector` inner class

Locate the `FitDataCollector` class (around line 188). After the existing `rawHeartRates` field, add:

```java
// Additional per-record streams — parallel to rawTimestamps (null where field absent)
private final List<Double>  rawDistances = new ArrayList<>();
private final List<Double>  rawAltitudes = new ArrayList<>();
private final List<Double>  rawSpeeds    = new ArrayList<>();  // m/s
private final List<Integer> rawCadences  = new ArrayList<>();
private final List<Integer> rawPowers    = new ArrayList<>();
private final List<Double>  rawLats      = new ArrayList<>();  // degrees
private final List<Double>  rawLons      = new ArrayList<>();  // degrees

// Semicircle → degree conversion factor
private static final double SEMICIRCLE_TO_DEG = 180.0 / Math.pow(2, 31);

// Computed after finalizeData()
private List<Double>  distances  = new ArrayList<>();
private List<Double>  altitudes  = new ArrayList<>();
private List<Double>  speeds     = new ArrayList<>();
private List<Integer> cadences   = new ArrayList<>();
private List<Integer> powers     = new ArrayList<>();
private List<Double>  latitudes  = new ArrayList<>();
private List<Double>  longitudes = new ArrayList<>();
```

#### Step 4.2 — Rewrite `handleRecord()` to collect all streams

Replace the existing `handleRecord()` method:

```java
private void handleRecord(Mesg mesg) {
    Field tsField = mesg.getField("timestamp");
    if (tsField == null || tsField.getValue() == null) return;
    Long ts = tsField.getLongValue();
    if (ts == null) return;

    rawTimestamps.add(ts);

    // Heart rate
    rawHeartRates.add(extractInt(mesg, "heart_rate"));

    // Distance (meters cumulative)
    rawDistances.add(extractDouble(mesg, "distance"));

    // Altitude (meters)
    rawAltitudes.add(extractDouble(mesg, "altitude"));

    // Speed (m/s)
    rawSpeeds.add(extractDouble(mesg, "speed"));

    // Cadence (spm/rpm)
    rawCadences.add(extractInt(mesg, "cadence"));

    // Power (watts)
    rawPowers.add(extractInt(mesg, "power"));

    // GPS coordinates (semicircles → degrees)
    Double latSc = extractDouble(mesg, "position_lat");
    Double lonSc = extractDouble(mesg, "position_long");
    rawLats.add(latSc != null ? latSc * SEMICIRCLE_TO_DEG : null);
    rawLons.add(lonSc != null ? lonSc * SEMICIRCLE_TO_DEG : null);
}

private Double extractDouble(Mesg mesg, String fieldName) {
    Field f = mesg.getField(fieldName);
    return (f != null && f.getValue() != null) ? f.getDoubleValue() : null;
}

private Integer extractInt(Mesg mesg, String fieldName) {
    Field f = mesg.getField(fieldName);
    if (f == null || f.getValue() == null) return null;
    Double v = f.getDoubleValue();
    return v != null ? v.intValue() : null;
}
```

#### Step 4.3 — Extend `finalizeData()` to build output lists and apply distance fallback

Add after the existing `timeSeconds`/`heartRates` loop:

```java
// Build output lists aligned with timeSeconds
distances  = new ArrayList<>(rawDistances);
altitudes  = new ArrayList<>(rawAltitudes);
speeds     = new ArrayList<>(rawSpeeds);
cadences   = new ArrayList<>(rawCadences);
powers     = new ArrayList<>(rawPowers);
latitudes  = new ArrayList<>(rawLats);
longitudes = new ArrayList<>(rawLons);

// Distance fallback for indoor/treadmill FIT files (no GPS distance)
boolean distanceAllNull = distances.stream().allMatch(Objects::isNull);
if (distanceAllNull) {
    // Derive cumulative distance from speed × Δt
    boolean hasAnySpeed = speeds.stream().anyMatch(Objects::nonNull);
    if (hasAnySpeed) {
        double cumulative = 0.0;
        for (int i = 0; i < rawTimestamps.size(); i++) {
            Double spd = speeds.get(i);
            if (spd != null && i > 0) {
                long dt = rawTimestamps.get(i) - rawTimestamps.get(i - 1);
                cumulative += spd * dt;
            }
            distances.set(i, cumulative);  // 0.0 at i=0 is valid (start of activity)
        }
        // Re-check: if still all null (first point is always 0.0 now), that's fine
    }
    // If no speed either, distances remains all-null → ActivityStream will not be saved
}
```

Also add `import java.util.Objects;` at the top of the file if not already present.

#### Step 4.4 — Add getters for new lists

After the existing `getHeartRates()` getter:

```java
public List<Double>  getDistances()  { return distances; }
public List<Double>  getAltitudes()  { return altitudes; }
public List<Double>  getSpeeds()     { return speeds; }
public List<Integer> getCadences()   { return cadences; }
public List<Integer> getPowers()     { return powers; }
public List<Double>  getLatitudes()  { return latitudes; }
public List<Double>  getLongitudes() { return longitudes; }
```

- [ ] **Step 4.5: Compile check**

```bash
cd backend && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4.6: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/CompletedTrainingService.java
git commit -m "feat: extend FitDataCollector to collect distance, altitude, speed, cadence, power and GPS streams"
```

---

### Task 5: Save ActivityStream After FIT Upload

**Files:**
- Modify: `backend/src/main/java/com/trainingsplan/service/CompletedTrainingService.java`

#### Step 5.1 — Add imports

At the top of `CompletedTrainingService.java`, add:

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainingsplan.entity.ActivityStream;
import com.trainingsplan.repository.ActivityStreamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

#### Step 5.2 — Add class-level fields

After the existing `@Autowired private TcxParsingService tcxParsingService;` line:

```java
@Autowired
private ActivityStreamRepository activityStreamRepository;

@Autowired
private ObjectMapper objectMapper;

private static final Logger log = LoggerFactory.getLogger(CompletedTrainingService.class);
```

#### Step 5.3 — Add helper methods

Before the `getCompletedTrainingsByDate()` method, add:

```java
/**
 * Serializes a list to JSON. Returns null if the list is null, empty, or contains only nulls
 * (to avoid storing meaningless [null,null,...] arrays that cause NPE during stream parsing).
 */
private String toJson(List<?> list) {
    if (list == null || list.isEmpty()) return null;
    boolean allNull = list.stream().allMatch(Objects::isNull);
    if (allNull) return null;
    try {
        return objectMapper.writeValueAsString(list);
    } catch (Exception e) {
        log.warn("Failed to serialize stream list: {}", e.getMessage());
        return null;
    }
}

/**
 * Builds [[lat,lon],...] JSON, skipping pairs where either coordinate is null.
 */
private String buildLatlngJson(List<Double> lats, List<Double> lons) {
    if (lats == null || lons == null || lats.isEmpty()) return null;
    java.util.List<double[]> pairs = new java.util.ArrayList<>();
    for (int i = 0; i < Math.min(lats.size(), lons.size()); i++) {
        Double lat = lats.get(i);
        Double lon = lons.get(i);
        if (lat != null && lon != null) {
            pairs.add(new double[]{lat, lon});
        }
    }
    if (pairs.isEmpty()) return null;
    try {
        return objectMapper.writeValueAsString(pairs);
    } catch (Exception e) {
        log.warn("Failed to serialize latlng stream: {}", e.getMessage());
        return null;
    }
}

/**
 * Saves an ActivityStream for the given completed training from FIT collector data.
 * Skipped if distanceJson would be null (service requires distance as x-axis).
 */
private void saveActivityStream(CompletedTraining savedTraining, FitDataCollector collector) {
    try {
        String distanceJson = toJson(collector.getDistances());
        if (distanceJson == null) {
            log.debug("No distance stream for activity {}, skipping ActivityStream save", savedTraining.getId());
            return;
        }
        ActivityStream stream = new ActivityStream();
        stream.setCompletedTraining(savedTraining);
        stream.setFetchedAt(LocalDateTime.now());
        stream.setDistanceJson(distanceJson);
        stream.setTimeSecondsJson(toJson(collector.getTimeSeconds()));
        stream.setHeartrateJson(toJson(collector.getHeartRates()));
        stream.setAltitudeJson(toJson(collector.getAltitudes()));
        stream.setVelocitySmoothJson(toJson(collector.getSpeeds()));
        stream.setCadenceJson(toJson(collector.getCadences()));
        stream.setPowerJson(toJson(collector.getPowers()));
        stream.setLatlngJson(buildLatlngJson(collector.getLatitudes(), collector.getLongitudes()));
        activityStreamRepository.save(stream);
    } catch (Exception e) {
        log.warn("Failed to save ActivityStream for completedTrainingId={}: {}", savedTraining.getId(), e.getMessage());
    }
}
```

#### Step 5.4 — Call `saveActivityStream` in `uploadAndParseFitFile`

In `uploadAndParseFitFile(MultipartFile, LocalDate, Long)`, after `completedTrainingRepository.save(training)`:

```java
CompletedTraining savedTraining = completedTrainingRepository.save(training);

saveActivityStream(savedTraining, collector);   // ← add this line
```

- [ ] **Step 5.5: Compile check**

```bash
cd backend && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5.6: Manual smoke test**

Start the backend and upload a real `.fit` file:

```bash
# Start backend
cd backend && mvn spring-boot:run

# In a separate terminal — upload a FIT file (requires auth token)
curl -s -X POST http://localhost:8080/api/completed-trainings/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@your_activity.fit" \
  -F "date=2025-01-01" | python -m json.tool
```

Then fetch the streams:

```bash
curl -s http://localhost:8080/api/completed-trainings/ACTIVITY_ID/streams \
  -H "Authorization: Bearer YOUR_TOKEN" | python -m json.tool
```

Expected: JSON response with `distancePoints`, `heartRate`, `altitude`, `paceSecondsPerKm` arrays, and the new `cadence`/`power` fields. Flags `hasCadence` / `hasPower` reflect whether the FIT file contained those streams.

- [ ] **Step 5.7: Commit**

```bash
git add backend/src/main/java/com/trainingsplan/service/CompletedTrainingService.java
git commit -m "feat: persist ActivityStream from FIT upload (distance, HR, altitude, pace, cadence, power, GPS)"
```

---

## Chunk 2: Frontend

### Task 6: ActivityStreamDto Interface

**Files:**
- Modify: `frontend/src/app/services/activity.service.ts`

- [ ] **Step 6.1: Add cadence and power fields**

In `activity.service.ts`, extend the `ActivityStreamDto` interface:

```typescript
export interface ActivityStreamDto {
  completedTrainingId: number;
  distancePoints: number[];
  heartRate: (number | null)[] | null;
  altitude: (number | null)[] | null;
  paceSecondsPerKm: (number | null)[] | null;
  cadence: (number | null)[] | null;
  power: (number | null)[] | null;
  hasHeartRate: boolean;
  hasAltitude: boolean;
  hasPace: boolean;
  hasCadence: boolean;
  hasPower: boolean;
}
```

- [ ] **Step 6.2: Compile check**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -20
```

Expected: no errors

- [ ] **Step 6.3: Commit**

```bash
git add frontend/src/app/services/activity.service.ts
git commit -m "feat: add cadence and power fields to ActivityStreamDto interface"
```

---

### Task 7: Unified Multi-Stream Chart

**Files:**
- Modify: `frontend/src/app/components/activity-detail/activity-detail.ts`
- Modify: `frontend/src/app/components/activity-detail/activity-detail.html`
- Modify: `frontend/src/app/components/activity-detail/activity-detail.scss`

#### Step 7.1 — Remove old hover state from `activity-detail.ts`

The existing component has hover fields and methods that conflict with the new implementation. **Remove** the following from the component class:

```typescript
// REMOVE these fields:
hoverIndex: number | null = null;
hoverPixelX: number | null = null;
hoverSvgX: number | null = null;
hoverSvgY: number | null = null;

// REMOVE these methods (entire method bodies):
onChartMouseMove(event: MouseEvent): void { ... }   // existing 1-arg version
```

Also add `signal` to the import from `@angular/core`:
```typescript
import { Component, OnInit, inject, ChangeDetectorRef, signal } from '@angular/core';
```

#### Step 7.2 — Add stream infrastructure to `activity-detail.ts`

After the existing `streams: ActivityStreamDto | null = null;` field, add:

```typescript
// Stream registry — defines each stream's display properties
readonly streamDescriptors = [
  { key: 'heartRate',        hasKey: 'hasHeartRate', label: 'HR',       unit: 'bpm', color: '#ef4444', axis: 'left'  as const, invert: false },
  { key: 'altitude',         hasKey: 'hasAltitude',  label: 'Altitude', unit: 'm',   color: '#60a5fa', axis: 'right' as const, invert: false },
  { key: 'paceSecondsPerKm', hasKey: 'hasPace',      label: 'Pace',     unit: '/km', color: '#4ade80', axis: 'left'  as const, invert: true  },
  { key: 'cadence',          hasKey: 'hasCadence',   label: 'Cadence',  unit: 'spm', color: '#fb923c', axis: 'right' as const, invert: false },
  { key: 'power',            hasKey: 'hasPower',     label: 'Power',    unit: 'W',   color: '#b9f20d', axis: 'left'  as const, invert: false },
] as const;

// Signal — Angular change detection reacts when .set() is called
activeStreams = signal<Set<string>>(new Set(['heartRate', 'altitude']));

tooltipIndex: number | null = null;
tooltipPixelX: number | null = null;

// Cadence unit: 'rpm' for cycling, 'spm' otherwise
get cadenceUnit(): string {
  return this.activity?.sport?.toLowerCase().includes('cycling') ||
         this.activity?.sport?.toLowerCase().includes('bike') ? 'rpm' : 'spm';
}

toggleStream(key: string): void {
  // Signals require creating a new Set to trigger change detection
  const next = new Set(this.activeStreams());
  if (next.has(key)) { next.delete(key); } else { next.add(key); }
  this.activeStreams.set(next);
}
```

#### Step 7.3 — Add chart computation helpers to `activity-detail.ts`

Add these methods to the component class:

```typescript
/** Returns the data array for a stream key. */
getStreamData(key: string): (number | null)[] | null {
  if (!this.streams) return null;
  return (this.streams as any)[key] as (number | null)[] | null;
}

/** Returns true if the stream has data (via has-flag). */
streamAvailable(hasKey: string): boolean {
  return this.streams ? !!(this.streams as any)[hasKey] : false;
}

/** Returns min/max of a nullable number array, or null if empty. */
streamRange(data: (number | null)[]): { min: number; max: number } | null {
  const vals = data.filter((v): v is number => v !== null);
  if (vals.length === 0) return null;
  return { min: Math.min(...vals), max: Math.max(...vals) };
}

/** Builds SVG path string for one stream. */
buildStreamPath(
  data: (number | null)[],
  range: { min: number; max: number },
  plotH: number,
  plotW: number,
  invert: boolean
): string {
  const dist = this.streams!.distancePoints;
  const maxDist = dist[dist.length - 1] || 1;
  const span = range.max - range.min || 1;
  const parts: string[] = [];
  let penUp = true;
  for (let i = 0; i < data.length; i++) {
    const v = data[i];
    if (v === null || v === undefined) { penUp = true; continue; }
    const x = (dist[i] / maxDist) * plotW;
    const norm = invert ? (range.max - v) / span : (v - range.min) / span;
    const y = plotH - norm * plotH;
    parts.push(`${penUp ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`);
    penUp = false;
  }
  return parts.join(' ');
}

/** Returns 5 tick labels for a Y-axis. */
yAxisTicks(
  range: { min: number; max: number },
  plotH: number,
  invert: boolean,
  isPace: boolean
): { y: number; label: string }[] {
  const ticks = [];
  for (let i = 0; i <= 4; i++) {
    const frac = i / 4;
    const val = range.min + frac * (range.max - range.min);
    const y = invert
      ? plotH - ((range.max - val) / (range.max - range.min || 1)) * plotH
      : plotH - frac * plotH;
    ticks.push({ y, label: isPace ? this.formatPace(Math.round(val)) : Math.round(val).toString() });
  }
  return ticks;
}

/** Streams that have data AND are toggled on. */
get availableActiveStreams() {
  const active = this.activeStreams();
  return this.streamDescriptors.filter(d => this.streamAvailable(d.hasKey) && active.has(d.key));
}

/** All streams that have data (for legend). */
get availableStreams() {
  return this.streamDescriptors.filter(d => this.streamAvailable(d.hasKey));
}

/** Left-side active streams (for Y-axis rendering). */
get leftActiveStreams() {
  return this.availableActiveStreams.filter(d => d.axis === 'left');
}

/** Right-side active streams (for Y-axis rendering). */
get rightActiveStreams() {
  return this.availableActiveStreams.filter(d => d.axis === 'right');
}

/** SVG plot-area left offset in viewBox units (55 per left axis). */
get plotOffsetX(): number { return this.leftActiveStreams.length * 55; }

/** SVG plot-area width in viewBox units. */
get plotW(): number { return 800 - this.plotOffsetX - this.rightActiveStreams.length * 55; }

readonly plotH = 200;

onChartMouseMove(event: MouseEvent): void {
  const rect = (event.currentTarget as SVGElement).getBoundingClientRect();
  const relX = event.clientX - rect.left;
  const dist = this.streams?.distancePoints;
  if (!dist?.length) return;
  const maxDist = dist[dist.length - 1] || 1;
  const targetDist = (relX / rect.width) * maxDist;
  let closest = 0;
  let minDiff = Math.abs(dist[0] - targetDist);
  for (let i = 1; i < dist.length; i++) {
    const diff = Math.abs(dist[i] - targetDist);
    if (diff < minDiff) { minDiff = diff; closest = i; }
  }
  this.tooltipIndex = closest;
  this.tooltipPixelX = relX;
  this.cdr.detectChanges();
}

onChartMouseLeave(): void {
  this.tooltipIndex = null;
  this.tooltipPixelX = null;
  this.cdr.detectChanges();
}

hoverX(idx: number): number {
  if (!this.streams) return 0;
  const dist = this.streams.distancePoints;
  return (dist[idx] / (dist[dist.length - 1] || 1)) * this.plotW;
}

formatPace(seconds: number): string {
  if (!seconds || seconds <= 0) return '—';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}
```

#### Step 7.4 — Replace chart section in `activity-detail.html`

Find the existing performance chart section (look for `class="performance-chart"` or similar) and replace the **entire** section with:

```html
<!-- Unified Multi-Stream Chart -->
@if (streams) {
<section class="perf-chart-section">

  <!-- Legend top-right -->
  <div class="chart-legend">
    @for (d of availableStreams; track d.key) {
      <button
        class="legend-pill"
        [class.legend-pill--active]="activeStreams().has(d.key)"
        [style.--pill-color]="d.color"
        (click)="toggleStream(d.key)"
      >
        <span class="legend-dot"></span>
        {{ d.key === 'cadence' ? 'Cadence' : d.label }}
        <span class="legend-unit">{{ d.key === 'cadence' ? cadenceUnit : d.unit }}</span>
      </button>
    }
  </div>

  <!-- SVG Chart -->
  <div class="chart-outer">
    <svg
      class="chart-svg"
      viewBox="0 0 800 240"
      preserveAspectRatio="none"
      (mousemove)="onChartMouseMove($event)"
      (mouseleave)="onChartMouseLeave()"
    >
      <g [attr.transform]="'translate(' + plotOffsetX + ',20)'">

        <!-- X-axis baseline -->
        <line x1="0" [attr.y1]="plotH" [attr.x2]="plotW" [attr.y2]="plotH"
              stroke="rgba(255,255,255,0.1)" stroke-width="1"/>

        <!-- Stream paths -->
        @for (d of availableActiveStreams; track d.key) {
          @let data = getStreamData(d.key);
          @let range = data ? streamRange(data) : null;
          @if (data && range) {
            <path
              [attr.d]="buildStreamPath(data, range, plotH, plotW, d.invert)"
              [attr.stroke]="d.color"
              fill="none"
              stroke-width="1.5"
              stroke-linejoin="round"
            />
          }
        }

        <!-- Hover hairline -->
        @if (tooltipIndex !== null) {
          <line
            [attr.x1]="hoverX(tooltipIndex)" y1="0"
            [attr.x2]="hoverX(tooltipIndex)" [attr.y2]="plotH"
            stroke="rgba(255,255,255,0.3)" stroke-width="1" stroke-dasharray="4,3"
          />
        }

        <!-- Left Y-axes -->
        @for (d of leftActiveStreams; track d.key; let i = $index) {
          @let data = getStreamData(d.key);
          @let range = data ? streamRange(data) : null;
          @if (range) {
            <g [attr.transform]="'translate(' + (-(i + 1) * 55) + ',0)'">
              <line x1="50" y1="0" x2="50" [attr.y2]="plotH"
                    [attr.stroke]="d.color" stroke-opacity="0.3" stroke-width="1"/>
              @for (tick of yAxisTicks(range, plotH, d.invert, d.key === 'paceSecondsPerKm'); track tick.y) {
                <text x="46" [attr.y]="tick.y + 4" text-anchor="end"
                      font-size="10" [attr.fill]="d.color" fill-opacity="0.8">{{ tick.label }}</text>
              }
              <text x="42" y="-8" text-anchor="middle" font-size="9"
                    [attr.fill]="d.color" fill-opacity="0.6">{{ d.unit }}</text>
            </g>
          }
        }

        <!-- Right Y-axes -->
        @for (d of rightActiveStreams; track d.key; let i = $index) {
          @let data = getStreamData(d.key);
          @let range = data ? streamRange(data) : null;
          @if (range) {
            <g [attr.transform]="'translate(' + (plotW + i * 55) + ',0)'">
              <line x1="5" y1="0" x2="5" [attr.y2]="plotH"
                    [attr.stroke]="d.color" stroke-opacity="0.3" stroke-width="1"/>
              @for (tick of yAxisTicks(range, plotH, d.invert, false); track tick.y) {
                <text x="9" [attr.y]="tick.y + 4" text-anchor="start"
                      font-size="10" [attr.fill]="d.color" fill-opacity="0.8">{{ tick.label }}</text>
              }
              <text x="9" y="-8" text-anchor="start" font-size="9"
                    [attr.fill]="d.color" fill-opacity="0.6">{{ d.unit }}</text>
            </g>
          }
        }

      </g>
    </svg>

    <!-- Hover Tooltip -->
    @if (tooltipIndex !== null) {
      <div class="chart-tooltip" [style.left.px]="tooltipPixelX">
        <div class="tooltip-dist">{{ streams.distancePoints[tooltipIndex].toFixed(2) }} km</div>
        @for (d of availableActiveStreams; track d.key) {
          @let val = getStreamData(d.key)?.[tooltipIndex];
          @if (val !== null && val !== undefined) {
            <div class="tooltip-row">
              <span class="tooltip-dot" [style.background]="d.color"></span>
              <span class="tooltip-label">{{ d.label }}</span>
              <span class="tooltip-val">
                {{ d.key === 'paceSecondsPerKm' ? formatPace(+val) : val }}
                {{ d.key === 'cadence' ? cadenceUnit : d.unit }}
              </span>
            </div>
          }
        }
      </div>
    }
  </div>

</section>
}

#### Step 7.4 — Add chart styles to `activity-detail.scss`

Append to the end of `activity-detail.scss`:

```scss
// ── Unified Stream Chart ────────────────────────────────────────────────────

.perf-chart-section {
  margin-top: 1.5rem;
  position: relative;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  justify-content: flex-end;
}

.legend-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.75rem;
  border-radius: 9999px;
  border: 1px solid var(--pill-color);
  background: transparent;
  color: #94a3b8;
  font-size: 0.72rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &--active {
    background: color-mix(in srgb, var(--pill-color) 15%, transparent);
    color: var(--pill-color);
  }

  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--pill-color);
    flex-shrink: 0;
  }

  .legend-unit {
    font-size: 0.65rem;
    opacity: 0.7;
  }
}

.chart-outer {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0.75rem;
  overflow: hidden;
}

.chart-svg {
  display: block;
  width: 100%;
  height: 280px;
  cursor: crosshair;
}

.chart-tooltip {
  position: absolute;
  top: 8px;
  transform: translateX(-50%);
  background: rgba(15, 17, 10, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 0.5rem;
  padding: 0.5rem 0.75rem;
  pointer-events: none;
  min-width: 120px;
  backdrop-filter: blur(8px);
  z-index: 10;

  .tooltip-dist {
    font-size: 0.7rem;
    font-weight: 700;
    color: rgba(255,255,255,0.5);
    margin-bottom: 0.35rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .tooltip-row {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.78rem;
    margin: 0.15rem 0;
  }

  .tooltip-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .tooltip-label {
    color: rgba(255,255,255,0.5);
    flex: 1;
  }

  .tooltip-val {
    font-weight: 700;
    color: #f1f5f9;
  }
}
```

- [ ] **Step 7.5: Compile check**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -30
```

Fix any TypeScript errors. The most likely issue is Angular `@let` syntax — if not supported, move computed values to getter properties in the component.

- [ ] **Step 7.6: Visual smoke test**

```bash
cd frontend && npm start
```

Navigate to an activity that has stream data (`/activities/:id`). Expected:
- Legend pills visible top-right (HR and Altitude active by default)
- Clicking a pill toggles the stream on/off
- Chart shows colored lines for each active stream
- Hovering shows hairline + tooltip with values

- [ ] **Step 7.7: Commit**

```bash
git add frontend/src/app/components/activity-detail/activity-detail.ts
git add frontend/src/app/components/activity-detail/activity-detail.html
git add frontend/src/app/components/activity-detail/activity-detail.scss
git commit -m "feat: unified multi-stream chart with toggleable legend and multi-Y-axes in activity detail"
```

---

## Final Verification

- [ ] Upload a FIT file via the Upload screen → navigate to the new activity → streams chart appears
- [ ] Toggle each available stream on/off → Y-axis appears/disappears
- [ ] Hover over chart → tooltip shows all active stream values at that distance
- [ ] Pace stream renders inverted (fast = high on chart)
- [ ] Existing Strava activities still show HR + altitude (no regression)
- [ ] Indoor FIT without distance: no streams chart shown (graceful fallback)
