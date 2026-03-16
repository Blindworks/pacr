# Design Spec: FIT File Full Stream Capture & Unified Multi-Stream Chart

**Date:** 2026-03-16
**Status:** Approved (v2 — post-review)

---

## Overview

When a user uploads a FIT file, all per-second data streams (heart rate, altitude, speed/pace, cadence, power, GPS) are captured from the record messages and persisted in the existing `ActivityStream` entity. The activity detail view displays all available streams in a single unified SVG chart with multiple Y-axes and a toggleable legend.

---

## Backend

### 1. `FitDataCollector` — Extended Stream Collection

Extend `handleRecord()` in the inner `FitDataCollector` class to collect additional parallel lists alongside the existing `rawTimestamps` and `rawHeartRates`.

**Critical rule:** Every new raw list must receive a `null` append in the same `handleRecord()` call as `rawTimestamps`, unconditionally, before any early-return. This guarantees all lists stay parallel and equal-length.

| List | FIT Field | Unit | Conversion |
|---|---|---|---|
| `rawDistances` | `distance` | meters | none |
| `rawAltitudes` | `altitude` | meters | none |
| `rawSpeeds` | `speed` | m/s | none (pace derived = 1000/speed) |
| `rawCadences` | `cadence` | rpm/spm | none |
| `rawPowers` | `power` | watts | none |
| `rawLats` | `position_lat` | semicircles | `× (180.0 / Math.pow(2, 31))` → degrees |
| `rawLons` | `position_long` | semicircles | `× (180.0 / Math.pow(2, 31))` → degrees |

`finalizeData()` converts these raw lists to relative-time aligned lists (same index alignment as existing `timeSeconds`/`heartRates`).

**Distance fallback for indoor/treadmill activities:** If `rawDistances` is all-null after parsing (indoor FIT with no GPS distance), derive cumulative distance from speed: iterate `rawSpeeds` and accumulate `speed × Δt` (seconds between adjacent timestamps). If speed is also absent, distance remains all-null and no `ActivityStream` is saved.

New public getters: `getDistances()`, `getAltitudes()`, `getSpeeds()`, `getCadences()`, `getPowers()`, `getLatitudes()`, `getLongitudes()`. All return `List<Double>` (nullable elements).

### 2. `uploadAndParseFitFile()` — Persist ActivityStream

Add `@Autowired private ActivityStreamRepository activityStreamRepository;` as a class-level field in `CompletedTrainingService` (alongside the existing 6 `@Autowired` fields).

After `completedTrainingRepository.save(training)`, build and save an `ActivityStream`:

```java
ActivityStream stream = new ActivityStream();
stream.setCompletedTraining(savedTraining);   // FK must be set before save
stream.setFetchedAt(LocalDateTime.now());
stream.setTimeSecondsJson(toJson(collector.getTimeSeconds()));
stream.setHeartrateJson(toJson(collector.getHeartRates()));
stream.setDistanceJson(toJson(collector.getDistances()));
stream.setAltitudeJson(toJson(collector.getAltitudes()));
stream.setVelocitySmoothJson(toJson(collector.getSpeeds()));
stream.setCadenceJson(toJson(collector.getCadences()));
stream.setPowerJson(toJson(collector.getPowers()));
stream.setLatlngJson(buildLatlngJson(collector.getLatitudes(), collector.getLongitudes()));
activityStreamRepository.save(stream);
```

Only execute if at least one stream list (other than timestamps) is non-empty with at least one non-null value. Wrap in try/catch; on failure log a warning and continue — `CompletedTraining` must still be saved.

**Null-array rule:** When calling `toJson()`, if the list is entirely null-valued (all elements null), pass `null` to the setter instead of serializing — so the column stays SQL `NULL` rather than storing `[null,null,...]`. This prevents an unboxing `NullPointerException` in `ActivityStreamService.getStreamDto()` when it iterates the distance array.

**Distance-required rule:** If `distanceJson` would be `null` after applying both the fallback (speed × Δt) and the null-array rule, **skip saving the `ActivityStream` row entirely**, regardless of whether other stream columns (HR, altitude, etc.) are non-null. The `ActivityStreamService` requires a non-null distance array as the x-axis; a row with `distance_json = NULL` would cause `getStreamDto()` to return `Optional.empty()` and silently discard all other streams.

`toJson(List<?>)` serializes with `ObjectMapper` (same instance/pattern as used in `ActivityStreamService`).

`buildLatlngJson()` produces `[[lat1,lon1],[lat2,lon2],...]`, skipping pairs where either coordinate is null.

**GPS storage note:** GPS coordinates are stored for future map rendering. The array for a 1-hour run at 1 Hz is ~3600 pairs (approx. 80 KB JSON) — acceptable in LONGTEXT.

### 3. `ActivityStream` Entity — New Columns

Add two nullable fields:

```java
@Column(name = "cadence_json", columnDefinition = "LONGTEXT")
private String cadenceJson;

@Column(name = "power_json", columnDefinition = "LONGTEXT")
private String powerJson;
```

Add getters/setters.

### 4. Liquibase Migration

Next migration number: **054** (last is `053-extend-body-metrics-for-daily.xml`).

File: `backend/src/main/resources/db/changelog/changes/054-add-activity-stream-cadence-power.xml`

Use two separate changesets — one per column — each with its own `<preConditions>`:

```xml
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
```

Add include in `db.changelog-master.xml`.

### 5. `ActivityStreamDto` — New Fields

Keep `Long completedTrainingId` (boxed) to match the existing record signature.

Add 4 new fields to the existing record:

```java
Integer[] cadence,       // spm/rpm per sample, nullable elements
Integer[] power,         // watts per sample, nullable elements
boolean hasCadence,
boolean hasPower
```

### 6. `ActivityStreamService` — Parse Cadence, Power & Fix `has*` Flags

**Consistent `has*` logic:** Use `anyNonNull` check for all streams (both existing and new), not string-presence. A JSON array of all-nulls must produce `has* = false`.

Helper:
```java
private boolean anyNonNull(Object[] arr) {
    return arr != null && Arrays.stream(arr).anyMatch(Objects::nonNull);
}
```

Apply to: `hasHeartRate`, `hasAltitude`, `hasPace`, `hasCadence`, `hasPower`. Call `anyNonNull` on the downsampled typed array (e.g., `heartRate`, `altitude`, `paceSecondsPerKm`) **after the sampling loop**, before constructing the DTO.

Parse `cadenceJson` and `powerJson` identically to the existing heartrate parsing. Downsample with the same step-based approach (same step value derived from `distanceJson` length).

---

## Frontend

### 1. `ActivityStreamDto` Interface (`activity.service.ts`)

Add to existing interface:
```typescript
cadence: (number | null)[] | null;
power: (number | null)[] | null;
hasCadence: boolean;
hasPower: boolean;
```

### 2. Unified Chart (`activity-detail`)

Replace the existing HR/Elevation overlay chart with a unified multi-stream SVG chart.

#### Stream Descriptors

```typescript
interface StreamDescriptor {
  key: 'heartRate' | 'altitude' | 'paceSecondsPerKm' | 'cadence' | 'power';
  hasKey: 'hasHeartRate' | 'hasAltitude' | 'hasPace' | 'hasCadence' | 'hasPower';
  label: string;
  unit: string;
  color: string;
  axisPosition: 'left' | 'right';
  invertAxis: boolean;   // true for pace (lower value = faster = higher on chart)
}
```

| key | label | unit | color | axis | invert |
|---|---|---|---|---|---|
| `heartRate` | HR | bpm | `#ef4444` | left | false |
| `altitude` | Altitude | m | `#60a5fa` | right | false |
| `paceSecondsPerKm` | Pace | /km | `#4ade80` | left | true |
| `cadence` | Cadence | spm/rpm | `#fb923c` | right | false |
| `power` | Power | W | `#b9f20d` | left | false |

Cadence unit: determine from `activity.sport` — if sport contains `"cycling"` or `"bike"` → `"rpm"`, otherwise → `"spm"`.

#### Active Streams State

```typescript
activeStreams = signal<Set<string>>(new Set(['heartRate', 'altitude']));
```

HR and altitude active by default (matches existing behavior). Only streams where `has* === true` are offered in the legend.

#### Legend (top-right)

Pill buttons for each available stream. Each pill: colored dot + label + unit. Click toggles `activeStreams`. Active pills have filled background, inactive are outlined.

#### Y-Axis Layout

Left axes stack outward left (offset = `leftIndex × 55px`). Right axes stack outward right.

Max 3 left axes, max 2 right axes = max 5 active streams simultaneously.

Left margin = `activeLeftCount × 55`, right margin = `activeRightCount × 55`, minimum left/right margin = 40px.

**Responsive note:** On containers narrower than 500px, collapse to maximum 2 active streams (hide lower-priority ones with a warning in the legend). Priority order for collapse: Power → Cadence → Pace → Altitude → HR.

Each axis: 5 evenly-spaced ticks, value labels in stream color, unit label rotated 90°.

#### SVG Path Rendering

For each active stream:

1. Compute `yMin = min(non-null values)`, `yMax = max(non-null values)`
2. Scale formula:
   - Normal: `y = plotHeight - ((val - yMin) / (yMax - yMin)) * plotHeight`
   - Inverted (pace): `y = plotHeight - ((yMax - val) / (yMax - yMin)) * plotHeight`
3. Skip null entries (lift pen with `M` instead of `L`)
4. Render `<path stroke=color fill=none stroke-width=1.5 stroke-linejoin=round>`

#### Hover Tooltip

On `mousemove` over the SVG, find the nearest `distancePoints` index by pixel position. Render:
- Vertical hairline across full chart height
- Floating box listing all active stream values with units
- X position clamps to chart edges to keep tooltip visible

---

## What Does NOT Change

- `GET /api/completed-trainings/{id}/streams` — endpoint unchanged
- `POST /api/completed-trainings/{id}/fetch-streams` (Strava) — unaffected
- GPX/TCX parsing — out of scope
- `CompletedTraining` entity — no new columns

---

## Implementation Checklist

- [ ] `FitDataCollector` extended with 7 new parallel stream lists (null appended unconditionally per record)
- [ ] Distance fallback from speed × Δt for indoor FIT files
- [ ] `ActivityStream` entity: `cadenceJson` + `powerJson` fields + getters/setters
- [ ] Liquibase `054-add-activity-stream-cadence-power.xml` with two separate changesets
- [ ] `054` include added to `db.changelog-master.xml`
- [ ] `uploadAndParseFitFile()`: `activityStreamRepository` field-injected, `setCompletedTraining(savedTraining)` called before save, wrapped in try/catch
- [ ] `ActivityStreamDto` record: 4 new fields, `Long completedTrainingId` kept
- [ ] `ActivityStreamService`: cadence/power parsed + downsampled, `has*` flags use `anyNonNull` consistently
- [ ] Frontend `ActivityStreamDto` interface: 4 new fields
- [ ] Unified chart replaces dual-overlay chart in `activity-detail`
- [ ] Legend with toggle, per-stream `has*` gating
- [ ] Multi-Y-axis with 55px offset, responsive collapse at <500px
- [ ] Pace axis inverted, explicit formula used
- [ ] Hover tooltip shows all active streams
