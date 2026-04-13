# Bulk Training Plan Import — JSON Format v2.0

**Date:** 2026-04-10
**Status:** Draft

## Context

PACR braucht massenhaft Trainingspläne (5K, 10K, HM, Marathon, 50K+) aus Büchern. Der Workflow:
Buchseite abfotografieren → ChatGPT parst Foto → generiert JSON v2.0 → Upload über bestehenden `/api/training-plans/upload-template` Endpoint.

Das bestehende Marathon-Format ist zu limitiert (keine Steps, keine Paces, keine Distanzen). Ein neues universelles Format wird benötigt, das alle Distanzen und Trainingstypen abdeckt und TrainingStep-Entities befüllt.

## JSON Schema v2.0

### Vollständiges Beispiel (Halbmarathon 1:33)

```json
{
  "format_version": "2.0",
  "plan": {
    "name": "Halbmarathon-Trainingsplan Zielzeit 1:33",
    "competitionType": "HALF_MARATHON",
    "targetTime": "1:33 Stunden",
    "prerequisites": "10 km in 45:00 min oder Halbmarathon in 1:40 h oder Marathon in 3:30 h",
    "totalWeeks": 10,
    "targetPaces": {
      "longRunPace": "5:46 min/km",
      "easyRunPace": "4:59 min/km",
      "halfMarathonPace": "4:24 min/km",
      "intervalPace1000m": "4:07 min",
      "intervalPace2000m": "8:30 min",
      "intervalPace400m": "1:34 min"
    },
    "weeks": [
      {
        "weekNumber": 1,
        "weeklyVolume": "76%",
        "totalKm": 47,
        "schedule": {
          "monday": {
            "name": "Langsamer Dauerlauf",
            "description": "10 km langsamer DL, GA1",
            "trainingType": "endurance",
            "intensityLevel": "low",
            "intensityPercent": 70,
            "estimatedDistanceMeters": 10000,
            "durationMinutes": 58,
            "benefit": "Grundlagenausdauer aufbauen",
            "steps": [
              {
                "stepType": "warmup",
                "title": "Einlaufen",
                "distanceMeters": 2000,
                "paceDisplay": "5:46 min/km"
              },
              {
                "stepType": "active",
                "title": "Hauptteil GA1",
                "distanceMeters": 8000,
                "paceDisplay": "5:46 min/km"
              }
            ]
          },
          "tuesday": {
            "name": "Krafttraining",
            "description": "Krafttraining",
            "trainingType": "strength",
            "intensityLevel": "rest",
            "intensityPercent": 0,
            "durationMinutes": 60,
            "benefit": "Verletzungsprävention und Laufökonomie"
          },
          "wednesday": {
            "name": "Lockerer Dauerlauf",
            "description": "10 km lockerer DL, GA2",
            "trainingType": "endurance",
            "intensityLevel": "medium",
            "intensityPercent": 80,
            "estimatedDistanceMeters": 10000,
            "durationMinutes": 50,
            "benefit": "Aerobe Kapazität verbessern"
          },
          "thursday": {
            "name": "Ruhetag",
            "trainingType": "rest",
            "intensityLevel": "rest",
            "intensityPercent": 0
          },
          "friday": {
            "name": "Lockerer Dauerlauf",
            "description": "12 km lockerer DL, GA1",
            "trainingType": "endurance",
            "intensityLevel": "medium",
            "intensityPercent": 80,
            "estimatedDistanceMeters": 12000,
            "durationMinutes": 69,
            "benefit": "Grundlagenausdauer"
          },
          "saturday": {
            "name": "Radfahren",
            "description": "2 h Radfahren, RECOM",
            "trainingType": "cycling",
            "intensityLevel": "recovery",
            "intensityPercent": 65,
            "durationMinutes": 120,
            "benefit": "Aktive Regeneration"
          },
          "sunday": {
            "name": "Langer Dauerlauf",
            "description": "15 km langsamer DL, GA1",
            "trainingType": "endurance",
            "intensityLevel": "low",
            "intensityPercent": 70,
            "estimatedDistanceMeters": 15000,
            "durationMinutes": 87,
            "benefit": "Ausdauergrundlage und mentale Stärke"
          }
        }
      },
      {
        "weekNumber": 4,
        "weeklyVolume": "87%",
        "totalKm": 34,
        "schedule": {
          "monday": {
            "name": "Krafttraining",
            "description": "Krafttraining",
            "trainingType": "strength",
            "intensityLevel": "rest",
            "intensityPercent": 0,
            "durationMinutes": 60,
            "benefit": "Verletzungsprävention und Laufökonomie"
          },
          "tuesday": {
            "name": "1000m-Intervalle",
            "description": "7 x 1000 m Intervalle, SB",
            "trainingType": "interval",
            "intensityLevel": "high",
            "intensityPercent": 90,
            "estimatedDistanceMeters": 12000,
            "durationMinutes": 55,
            "benefit": "VO2max und Laktatschwelle verbessern",
            "steps": [
              {
                "stepType": "warmup",
                "title": "Einlaufen",
                "distanceMeters": 2000,
                "paceDisplay": "5:30 min/km",
                "durationMinutes": 11
              },
              {
                "stepType": "interval",
                "title": "1000m schnell",
                "subtitle": "SB-Tempo",
                "distanceMeters": 1000,
                "paceDisplay": "4:07 min/km",
                "repetitions": 7,
                "highlight": true
              },
              {
                "stepType": "recovery",
                "title": "Trabpause",
                "durationMinutes": 3,
                "paceDisplay": "6:00 min/km",
                "muted": true
              },
              {
                "stepType": "cooldown",
                "title": "Auslaufen",
                "distanceMeters": 1000,
                "paceDisplay": "5:30 min/km"
              }
            ]
          },
          "wednesday": {
            "name": "Radfahren",
            "description": "2 h Radfahren, RECOM",
            "trainingType": "cycling",
            "intensityLevel": "recovery",
            "intensityPercent": 65,
            "durationMinutes": 120,
            "benefit": "Aktive Regeneration"
          },
          "thursday": {
            "name": "Lockerer Dauerlauf",
            "description": "6 km lockerer DL, GA2",
            "trainingType": "endurance",
            "intensityLevel": "medium",
            "intensityPercent": 80,
            "estimatedDistanceMeters": 6000,
            "durationMinutes": 30,
            "benefit": "Lockeres Auslaufen"
          },
          "friday": {
            "name": "Ruhetag",
            "trainingType": "rest",
            "intensityLevel": "rest",
            "intensityPercent": 0
          },
          "saturday": {
            "name": "10-km-Wettkampf",
            "description": "10-km-Wettkampf",
            "trainingType": "race",
            "intensityLevel": "high",
            "intensityPercent": 90,
            "estimatedDistanceMeters": 10000,
            "durationMinutes": 45,
            "benefit": "Wettkampfhärte und Tempogefühl"
          },
          "sunday": {
            "name": "Schwimmen",
            "description": "1 h Schwimmen, RECOM",
            "trainingType": "swimming",
            "intensityLevel": "recovery",
            "intensityPercent": 65,
            "durationMinutes": 60,
            "benefit": "Aktive Regeneration"
          }
        }
      }
    ]
  }
}
```

### Feld-Referenz

#### Plan-Level

| Feld | Typ | Pflicht | Beschreibung |
|------|-----|---------|--------------|
| `format_version` | string | ja | Immer `"2.0"` |
| `plan.name` | string | ja | Anzeigename des Plans |
| `plan.competitionType` | string | ja | Enum: `FIVE_K`, `TEN_K`, `HALF_MARATHON`, `MARATHON`, `FIFTY_K`, `HUNDRED_K`, `OTHER` |
| `plan.targetTime` | string | ja | Zielzeit, z.B. `"1:33 Stunden"` |
| `plan.prerequisites` | string | nein | Voraussetzungen aus dem Buch |
| `plan.totalWeeks` | int | ja | Gesamtanzahl Wochen |
| `plan.targetPaces` | object | nein | Zielpaces (beliebige key-value Paare) |

#### Wochen-Level

| Feld | Typ | Pflicht | Beschreibung |
|------|-----|---------|--------------|
| `weekNumber` | int | ja | 1 = erste Woche, N = Wettkampfwoche |
| `weeklyVolume` | string | nein | Trainingsvolumen in %, z.B. `"76%"` |
| `totalKm` | int | nein | Gesamtkilometer der Woche |

#### Training-Level (pro Tag)

| Feld | Typ | Pflicht | Beschreibung |
|------|-----|---------|--------------|
| `name` | string | ja | Kurzname des Trainings |
| `description` | string | nein | Detaillierte Beschreibung |
| `trainingType` | string | ja | `endurance`, `interval`, `strength`, `race`, `fartlek`, `swimming`, `cycling`, `rest`, `general` |
| `intensityLevel` | string | ja | `high`, `medium`, `low`, `recovery`, `rest` |
| `intensityPercent` | int | ja | 0-100, HF max % |
| `estimatedDistanceMeters` | int | nein | Geschätzte Gesamtdistanz in Metern |
| `durationMinutes` | int | nein | Dauer in Minuten |
| `benefit` | string | nein | Trainingszweck (max 100 Zeichen) |
| `steps` | array | nein | Trainingsschritte (siehe unten) |

#### Step-Level

| Feld | Typ | Pflicht | Beschreibung |
|------|-----|---------|--------------|
| `stepType` | string | ja | `warmup`, `active`, `interval`, `recovery`, `cooldown`, `stretch` |
| `title` | string | ja | Schritt-Titel |
| `subtitle` | string | nein | Zusatzinfo, z.B. "SB-Tempo" |
| `distanceMeters` | int | nein | Distanz dieses Schritts |
| `durationMinutes` | int | nein | Dauer in Minuten |
| `durationSeconds` | int | nein | Dauer in Sekunden (für kurze Intervalle) |
| `paceDisplay` | string | nein | Zielpace, z.B. `"4:07 min/km"` |
| `repetitions` | int | nein | Wiederholungen (für Intervalle) |
| `highlight` | bool | nein | Visuell hervorheben (Hauptbelastung) |
| `muted` | bool | nein | Visuell gedämpft (Pause) |

### Mapping auf bestehende Entities

| JSON-Feld | Entity-Feld | Anmerkung |
|-----------|-------------|-----------|
| `plan.name` | `TrainingPlan.name` | |
| `plan.targetTime` | `TrainingPlan.targetTime` | |
| `plan.prerequisites` | `TrainingPlan.prerequisites` | |
| `plan.competitionType` | `TrainingPlan.competitionType` | String → Enum |
| `training.name` | `Training.name` | |
| `training.description` | `Training.description` | |
| `weekNumber` | `Training.weekNumber` | |
| Tag-Key → DayOfWeek | `Training.dayOfWeek` | monday→MONDAY etc. |
| `trainingType` | `Training.trainingType` | |
| `intensityLevel` | `Training.intensityLevel` | |
| `intensityPercent` | `Training.intensityScore` | |
| `estimatedDistanceMeters` | `Training.estimatedDistanceMeters` | |
| `durationMinutes` | `Training.durationMinutes` | |
| `benefit` | `Training.benefit` | |
| `steps[].stepType` | `TrainingStep.stepType` | |
| `steps[].title` | `TrainingStep.title` | |
| `steps[].subtitle` | `TrainingStep.subtitle` | |
| `steps[].distanceMeters` | `TrainingStep.distanceMeters` | |
| `steps[].durationMinutes` | `TrainingStep.durationMinutes` | |
| `steps[].durationSeconds` | `TrainingStep.durationSeconds` | |
| `steps[].paceDisplay` | `TrainingStep.paceDisplay` | |
| `steps[].repetitions` | `TrainingStep.repetitions` | |
| `steps[].highlight` | `TrainingStep.highlight` | |
| `steps[].muted` | `TrainingStep.muted` | |

### targetPaces — Speicherung

`targetPaces` wird als JSON-String in `TrainingPlan.jsonContent` mitgespeichert. Kein neues DB-Feld nötig — die Info steht im gespeicherten JSON und kann bei Bedarf ausgelesen werden.

## Frontend-Änderungen: Admin Plan Upload

### Neuer Upload-Bereich in Plan-List

In `frontend/src/app/components/admin/plans/plan-list/plan-list.ts` wird eine Upload-Sektion ergänzt — analog zum GPX-Upload-Pattern in `admin-community-routes.ts`.

**UI-Elemente:**
- Upload-Card oben auf der Plan-List-Seite
- File-Input mit `accept=".json"` Filter
- Upload-Button (disabled wenn keine Datei gewählt)
- Erfolgs-/Fehler-Feedback
- Nach erfolgreichem Upload: Plan-Liste neu laden

**Ablauf:**
1. Admin wählt `.json`-Datei aus
2. Klick auf "Upload" → `TrainingPlanService.uploadTemplate(file)` aufrufen
3. Backend parst JSON, erstellt Plan + Trainings + Steps
4. Erfolg → Plan erscheint in der Liste, File-Input zurücksetzen
5. Fehler → Fehlermeldung anzeigen

### TrainingPlanService — Neue Methode

```typescript
uploadTemplate(file: File, name?: string, description?: string): Observable<TrainingPlan> {
  const formData = new FormData();
  formData.append('file', file);
  if (name) formData.append('name', name);
  if (description) formData.append('description', description);
  return this.http.post<TrainingPlan>(`${this.baseUrl}/upload-template`, formData);
}
```

### i18n-Keys (en.json + de.json)

```json
{
  "ADMIN": {
    "PLANS": {
      "UPLOAD_TITLE": "Upload Training Plan",
      "UPLOAD_HINT": "Upload a JSON file (format v2.0) with a complete training plan",
      "UPLOAD_SUCCESS": "Training plan uploaded successfully",
      "UPLOAD_ERROR": "Error uploading training plan",
      "SELECT_FILE": "Select JSON file",
      "UPLOAD": "Upload"
    }
  }
}
```

## Backend-Änderungen

### TrainingPlanService.java

Neue Methode `parseV2FormatAsTemplates(JsonNode, List<Training>, TrainingPlan)`:

1. Erkennung: `format_version == "2.0"` → neuer Parser
2. Plan-Metadaten auslesen und auf `TrainingPlan` setzen (`targetTime`, `prerequisites`, `competitionType`)
3. Wochen iterieren, pro Tag Training-Template erstellen
4. Steps-Array parsen und `TrainingStep`-Entities erzeugen via `training.addStep()`
5. Ruhetage (`trainingType == "rest"`) überspringen (kein Training-Record)
6. `trainingCount` berechnen (Anzahl nicht-Rest Trainings)

### Kein neuer Endpoint nötig

Der bestehende `POST /api/training-plans/upload-template` bleibt unverändert. Die Format-Erkennung in `parseAndCreateTemplates()` bekommt einen neuen Branch:

```java
if (root.has("format_version") && "2.0".equals(root.get("format_version").asText())) {
    parseV2FormatAsTemplates(root.get("plan"), templates, trainingPlan);
}
```

### Keine DB-Migration nötig

Alle Felder existieren bereits in `Training` und `TrainingStep`. Das neue JSON-Format füllt nur Felder, die bisher leer blieben.

## ChatGPT-Prompt für Foto-Parsing

Folgender Prompt kann bei ChatGPT zusammen mit dem Foto verwendet werden:

```
Analysiere diesen Trainingsplan aus einem Buch und konvertiere ihn in folgendes JSON-Format.

Regeln:
- format_version ist immer "2.0"
- competitionType: FIVE_K, TEN_K, HALF_MARATHON, MARATHON, FIFTY_K, HUNDRED_K, OTHER
- weekNumber: 1 = erste Trainingswoche, letzte Woche = Wettkampfwoche
- trainingType: endurance, interval, strength, race, fartlek, swimming, cycling, rest, general
- intensityLevel: wird aus intensityPercent abgeleitet: >=90%=high, >=75%=medium, >=65%=low, >0%=recovery, 0%=rest
- Ruhetage: trainingType="rest", intensityLevel="rest", intensityPercent=0
- Steps nur bei Intervallen, Fahrtspielen und komplexen Einheiten
- stepType: warmup, active, interval, recovery, cooldown, stretch
- Distanzen in Metern, Zeiten in Minuten
- benefit: kurzer Trainingszweck (max 100 Zeichen)
- Alle Texte auf Deutsch

[JSON-Schema hier einfügen]
```

## Verifikation

1. Einen Beispielplan als JSON v2.0 erstellen (Halbmarathon 1:33 aus dem Foto)
2. Backend starten, über `/api/training-plans/upload-template` hochladen
3. Prüfen: TrainingPlan-Metadaten korrekt (targetTime, prerequisites, competitionType)
4. Prüfen: Training-Records mit korrekten weekNumber, dayOfWeek, trainingType, intensityLevel
5. Prüfen: TrainingStep-Records bei Intervall-Trainings vorhanden
6. Plan einem Competition zuweisen → UserTrainingEntries prüfen
7. Frontend-Kalender prüfen: Trainings werden korrekt angezeigt mit Steps
