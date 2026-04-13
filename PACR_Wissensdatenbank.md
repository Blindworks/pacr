# PACR – Wissensdatenbank

> Umfassende Dokumentation der App **PACR** (Performance-Adaptive Cyclic Running) als Wissensgrundlage für Obsidian.
> Stand: 2026-04-09

---

## 1. Kernidee

**PACR** steht für *Performance-Adaptive Cyclic Running* und ist eine intelligente Web-Applikation zur Trainingsplanung und Rennvorbereitung für Läufer und Ausdauerathleten. Die App verschmilzt klassische, strukturierte Trainingspläne mit physiologischen Echtzeitdaten und KI-gesteuerten Empfehlungen zu einem adaptiven Coaching-System.

**Leitphilosophie:** *„Dein Körper treibt dein Training an, nicht umgekehrt."*

PACR kombiniert:
- **Wettkampforientierte Planung** – rückwärts vom Zieldatum aus
- **Live-Physiologie** – VO2max, Herzfrequenz, Schlaf, Asthma, Zyklusphase
- **Bioweather-Integration** – Wetter, Pollenflug, Luftqualität
- **KI-gestützte tägliche Anpassung** – LLM-gestützte Empfehlungen basierend auf Readiness und Kontext
- **Community-Aspekt** – Freunde, geteilte Routen, Trainer-Events, Gruppenläufe

**Zielgruppe:** Ambitionierte Hobby-Läufer, Wettkampfathleten, Trainer/Coaches und Athletinnen mit speziellen Anforderungen (z.B. Zyklus- oder Asthma-Tracking).

**Technologie-Stack:**
- **Backend:** Java 21, Spring Boot 3.2, Hibernate/JPA, MariaDB, Liquibase, Garmin FIT SDK 21.176.0, LangChain4j
- **Frontend:** Angular 19 (Standalone Components), Angular Material, TypeScript, RxJS, ngx-translate, Leaflet
- **Deployment:** Docker Compose, nginx, PWA-fähig
- **Domain:** `pacr.app`

---

## 2. Trainingsplan-Management

### 2.1 Wettkämpfe (Competitions)

Wettkämpfe sind der zentrale Anker jedes Trainingsplans. Ein Training wird rückwärts vom Wettkampfdatum geplant.

**Entities:** `Competition`, `CompetitionRegistration`, `CompetitionType`

**Unterstützte Typen:** Marathon, Halbmarathon, 10 km, 5 km, eigene Distanzen.

**Features:**
- Mehrere parallele Wettkämpfe möglich
- System-generierte Wettkämpfe (z.B. aus AI-Plänen) können in der UI gefiltert werden
- Lifecycle: *Wettkampf erstellen → Plan wählen/generieren → UserTrainingEntries erzeugen → Trainings absolvieren*

**Endpoints:**
- `GET /api/competitions` – Liste
- `POST /api/competitions` – Neu anlegen
- `PUT /api/competitions/{id}` – Bearbeiten
- `DELETE /api/competitions/{id}` – Löschen
- `GET /api/competitions/types` – Verfügbare Wettkampftypen

### 2.2 Trainingspläne als Templates

PACR nutzt ein **Template-basiertes Modell**: Trainings sind nicht fest an Daten gebunden, sondern an relative Wochen- und Tagesangaben. Dadurch lassen sich Pläne auf beliebige Athleten und Wettkämpfe anwenden.

**Entities:**
- `TrainingPlan` – Container für Trainings-Templates
- `Training` – einzelnes Template mit `weekNumber` (Int) + `dayOfWeek` (Enum)
- `UserTrainingEntry` – user-spezifischer, auf ein konkretes Datum berechneter Eintrag

**Historische Änderung:** Die frühere `TrainingWeek`-Tabelle wurde entfernt (Migrationen 035–039). Trainings leben jetzt ausschließlich als Template, konkrete Termine entstehen über `UserTrainingEntry`.

**Trainingstypen:** `speed`, `endurance`, `strength`, `race`, `fartlek`, `recovery`, `swimming`, `cycling`, `general`

**Intensitäten:** `high`, `medium`, `low`, `recovery`, `rest`

**Felder:** Name, Beschreibung, Dauer (Minuten), Startzeit, Typ, Intensität, optionale Rich-Text-Description.

**Upload-Format (JSON):**
```json
{
  "trainings": [
    {
      "name": "Intervalltraining",
      "description": "5x1000m Intervalle mit 3min Pause",
      "date": "2024-01-15",
      "type": "speed",
      "intensity": "high",
      "startTime": "18:00",
      "duration": 90
    }
  ]
}
```

**Endpoints:**
- `POST /api/training-plans/upload` – Template als JSON hochladen
- `GET /api/user-training-entries/calendar?from=&to=` – Kalenderansicht
- `PUT /api/user-training-entries/{id}` – Completion/Feedback aktualisieren

### 2.3 Kalenderansicht

- Wochenweise Darstellung (Montag–Sonntag)
- Geplante vs. abgeschlossene Trainings werden gegenübergestellt
- **Multi-Wettkampf-Unterstützung:** alle aktiven Competitions erscheinen im selben Kalender
- Statuswerte: `PENDING`, `COMPLETED`, `MISSED`
- Navigation zwischen Wochen mit Wochen-Jumper

### 2.4 Plan Adaptation (automatische Plananpassung)

- **Service:** `PlanAdaptationService` + `PlanAdaptationScheduler`
- Erkennt verpasste Workouts (7-Tage-Lookback) und niedrige Readiness (RED < 40)
- Erzeugt `PlanAdjustment`-Vorschläge mit Status `PENDING`, `ACCEPTED`, `REJECTED`
- **Typen:** `RESCHEDULE`, `INTENSITY_REDUCE`, `SKIP`

---

## 3. FIT/GPX/TCX-Upload und Aktivitätsauswertung

### 3.1 Unterstützte Formate

- **FIT** – Garmin FIT SDK 21.176.0 (lokal in Maven installiert, siehe `MAVEN_SETUP.md`)
- **GPX** – `GpxParsingService`
- **TCX** – `TcxParsingService`
- **Quellen:** manueller Upload, Strava, COROS-Webhook, Bot Runner

### 3.2 `CompletedTraining` Entity

Speichert jede absolvierte Einheit unabhängig vom Plan. Die Zuordnung zu geplanten Trainings erfolgt **per Datum**, nicht per Foreign Key.

**Felder:**
- User, TrainingDate, StartTime, Sport
- DistanceKm, DurationMinutes, MovingTimeSeconds
- AvgHeartRate, MaxHeartRate, AvgPace, AvgCadence, AvgPower
- ElevationGainM, ElevationLossM
- UploadDate, Source (`FIT`, `GPX`, `TCX`, `STRAVA`, `COROS`, `BOT`)

### 3.3 Activity Streams

- **Entity:** `ActivityStream` (JSON-Array mit Sekunden-Auflösung)
- Enthält pro Sekunde: `time`, `hr`, `pace`, `cadence`, `power`, `altitude`, `lat`, `lng`
- Basis für interaktive Multi-Achsen-Charts mit togglebaren Layern
- Grundlage für Karten-Gradienten (Pace, HR, Power)

**Endpoints:**
- `POST /api/completed-trainings/upload` – Multipart-Upload
- `GET /api/completed-trainings/by-date?date=YYYY-MM-DD`
- `GET /api/completed-trainings/{id}/streams`
- `PUT /api/trainings/{id}/feedback` – Completion + Rating

### 3.4 Interaktive Karte (Leaflet)

- Basis: Leaflet.js mit Custom Hotline-Canvas für Gradient-Linien
- SVG-Glow-Polyline für Kontrast in Light/Dark Theme
- Fullscreen-Dialog
- Kartenkacheln: CARTO `light_all` bzw. `dark_all` – theme-reaktiv
- Palette passt sich live dem Theme an

---

## 4. Analytics und Metriken

### 4.1 VO2max

- **Service:** `Vo2MaxService`
- **Methode:** Daniels/VDOT-Formel – `vo2max = vo2_at_pace / vo2_fraction`
- **Input:** Distanz (m) und Zeit (s) eines Laufs
- **Output:** VO2max in ml/kg/min, plus VDOT-Klassifikation (z.B. *World Class*, *Elite*)
- **Variante:** HF-korrigierter VO2max (nutzt `movingTimeSeconds` und `maxHeartRate`)
- Persistierung über `BodyMetric` (`VO2MAX`, `VO2MAX_HR_CORRECTED`)

### 4.2 Body Metrics und Messungen

- **`BodyMetric`** – longitudinales Tracking (VO2max, Fitness-Trends) mit `metric_type`-Enum und Wert
- **`BodyMeasurement`** – Gewicht (kg), Körperfett (%), optional Umfänge, Trend-Charts
- **`BloodPressure`** – systolisch/diastolisch, zeitbasierte Verläufe
- **Services:** `BodyMetricService`, `BodyMeasurementService`, `BloodPressureService`
- **Endpoints:** `/api/body-metrics`, `/api/body-measurements`, `/api/blood-pressure`

### 4.3 Daily Metrics

Tagesbasierte Aggregation aller relevanten Belastungs- und Recovery-Kennzahlen.

**Entity `DailyMetrics` (pro User & Tag):**
- `dailyStrain21` – 21-Tage-Strain
- `dailyTrimp` – Tages-TRIMP
- `efficiency_factor` (EF)
- `aerobic_decoupling_pct` – HF-Drift in %
- `acwr` – Acute:Chronic Workload Ratio
- `acwr_flag` – Enum: `GREEN`, `YELLOW`, `ORANGE`, `RED`
- `readiness_score` – 0–100, farbcodiert
- `coach_card_data` – JSON für tägliche Coach-Karte

**Service:** `DailyMetricsService` (idempotent, komplett neu berechenbar)
**Endpoints:** `GET /api/daily-metrics`, `POST /api/daily-metrics/compute-today`

### 4.4 ACWR – Acute:Chronic Workload Ratio

- Formel: `ACWR = 7-Tage-Last / 28-Tage-Last`
- Zonen:
  - **GREEN** 0.8–1.3 – optimal
  - **YELLOW** 1.3–1.5 – erhöht
  - **ORANGE** 1.5–2.0 – hohes Risiko
  - **RED** > 2.0 – sehr hohes Risiko
- Fließt direkt in den Readiness Score und die Trainingsempfehlungen ein

### 4.5 TRIMP (Bannister)

Formel: `TRIMP = Dauer(min) × (AHR / MaxHR) × e^(k × (HR/MaxHR − 0.5))`

- `k = 1.92` (männlich)
- `k = 1.67` (weiblich)
- Benötigt Ruhe-HF und Max-HF aus dem User-Profil
- Wird für Tages-TRIMP und Strain21 verwendet

**Services:** `TRIMPCalculator`, `StrainCalculator`, `LoadModelService`

### 4.6 Readiness Score

Composite-Wert von 0–100, Basis 80 Punkte, mit folgenden Abzügen:

| Bedingung | Abzug |
|---|---|
| ACWR RED | −35 (Empfehlung ≤ EASY) |
| ACWR ORANGE | −20 |
| Gestriger Strain > 14 | −15 |
| Aerobic Decoupling > 10 % | −10 |
| Aerobic Decoupling 5–10 % | −5 |
| Z4/Z5-Minuten in den letzten 2 Tagen > 20 | −10 |

**Empfehlungsschwellen:**
- `< 30` → REST
- `30–49` → EASY
- `50–69` → MODERATE
- `≥ 70` → HARD

**Farben:** Blau (exzellent) → Grün → Orange → Rot
**Service:** `ReadinessService`

### 4.7 Weitere Prognose-Services

- `RaceTimePredictionService` – Rennzeit-Vorhersagen
- `TrainingImpactService` / `TrainingImpactEngine` – Wirkung einzelner Workouts
- `MetricsKernelService` – zentrale Berechnungs-Engine
- `PersonalRecordService` – persönliche Bestzeiten

---

## 5. Körper- und Gesundheits-Tracking

### 5.1 Zyklus-Tracking (Cycle Tracking)

- **Entities:** `CycleSettings`, `CycleEntry`
- **Features:**
  - Zykluslänge & Periodendauer konfigurierbar
  - Phasen-Berechnung (Menstruation, Follikel, Ovulation, Luteal)
  - Tägliches Logging von Stimmung, Energie, Schlaf, Blutungsstärke, Symptomen
  - Integration in AI Trainer (Cycle-Adaptive Suggestions)
- **Opt-in:** per User-Setting aktivierbar

### 5.2 Asthma-Tracking

- **Entity:** `AsthmaEntry`
- Tägliche Symptomstärke, Medikation, Peak Flow
- Fließt in Bioweather-Kontext (Pollen/Luftqualität) ein

### 5.3 Schlafdaten

- **Service:** `SleepDataService`
- Import via Garmin CSV oder Smartwatch-Integration
- Schlafdauer und -qualität beeinflussen Readiness und AI-Empfehlungen

### 5.4 Körpermessungen

- Gewicht, Körperfett, optional Umfänge
- Blutdruck-Logging
- Charts für Trend-Analyse

---

## 6. Community Features

### 6.1 Freundschaften

- **Entity:** `Friendship` mit Status `PENDING`, `ACCEPTED`, `BLOCKED`
- **Discovery:**
  - Textsuche (Name/E-Mail)
  - **Nearby Friends:** Haversine-basiert mit 10/25/50/100 km Radius
  - Standort über Map-Picker oder Browser-Geolocation
  - Opt-in über `discoverable_by_others`
- **Endpoints:** `/api/friendships`, `/api/friendships/search`, `/api/friendships/search/nearby`, `/api/friendships/incoming`

### 6.2 Community Routes

Geteilte Laufstrecken mit Leaderboards.

- **Entities:** `CommunityRoute`, `RouteAttempt`
- **Features:**
  - Absolviertes Training als Community Route teilen (GPS wird übernommen)
  - Discovery nach Nähe (Haversine) mit Radius-Filter (Standard 25 km)
  - Reverse-Geocoding für Stadt/Ort
  - **Leaderboards** pro Route mit Filtern *All Time / This Month / This Week*
  - Automatisches Matching neuer Aktivitäten zu bestehenden Routen (Distanz ±30 %)
  - User-Opt-in (standardmäßig deaktiviert)
  - **Admin-Feature:** kuratierte Routen per GPX-Upload
- **Endpoints:** `/api/community-routes/nearby`, `/api/community-routes/{id}`, `/api/community-routes/mine`, `POST/DELETE /api/community-routes`, `POST /api/admin/community-routes`

### 6.3 Gruppen und Trainer-Events

- **Entities:** `GroupEvent`, `GroupEventRegistration`, `GroupEventException`
- **Rolle:** Trainer/Coaches erstellen Events, User registrieren sich
- **Event-Felder:** Name, Datum/Zeit, Ort (Geo), Distanz, Pace-Range, Schwierigkeit, Kapazität, Kosten
- **Wiederkehrende Events:** volle RRULE-Unterstützung nach RFC 5545
  - täglich, wöchentlich, zweiwöchentlich, monatlich (n-ter Wochentag), jährlich
  - Serien-Enddatum
  - Einzelne Vorkommen absagen (`GroupEventException`)
- **Per-Occurrence Registration:** Anmeldung für einzelne Termine einer Serie
- **Pace-Filter:** Events finden, die zum eigenen Tempo passen
- **PRO-Feature:** erfordert Subscription `PRO`
- **User-Endpoints:** `/api/group-events/nearby`, `/upcoming`, `POST /{id}/register`, `DELETE /{id}/unregister`
- **Trainer-Endpoints:** `/api/trainer/events`, `PUT /{id}/cancel-occurrence`, `GET /{id}/registrations`

### 6.4 Bot Runner System

Virtuelle Läufer für Community-Leben und Test-Szenarien.

- **Entities:** `User` mit `is_bot`-Flag, `BotProfile`
- **Bot-Profil-Felder:** Pace-Range, Distanz-Range, HR-Profil (min/max/Zonen-Anteile), Home-Koordinaten, Suchradius, Wochentagsset, Startzeit + Jitter, `includeInLeaderboard`
- **Scheduler:** `BotRunnerScheduler` (Minuten-Takt)
  - wählt nahe Community Route
  - kopiert den GPS-Track
  - generiert plausible HF-Serie
  - speichert `CompletedTraining` + `ActivityStream`
  - publisht `TrainingCompletedEvent` → automatisches Leaderboard-Matching
- **Security:** Bot-Accounts können sich nicht einloggen
- **Admin-Endpoints:** `/api/admin/bot-runners` (CRUD), `POST /{id}/run-now`

---

## 7. AI- und LLM-Features

### 7.1 Infrastruktur

- **Framework:** LangChain4j mit OpenAI (bzw. Claude-Provider)
- **Toggle:** Property `pacr.ai.enabled` (ConditionalOnProperty)
- **Pattern:** `ObjectProvider` für Graceful Degradation wenn AI deaktiviert
- **Sprache:** aktive User-Sprache wird in den Prompt eingebaut
- **Services:** `LLMClientService`, `PromptBuilder`, `DailyCoachPromptBuilder`

### 7.2 Daily Coach / AI Trainer

- **Zweck:** tägliche Trainingsempfehlung basierend auf vollem Kontext
- **Kontext-Inputs:**
  - heute geplantes Training
  - aktueller Readiness Score
  - Zyklusphase + Symptome (falls aktiviert)
  - Bioweather (Pollen, Luftqualität, Stress)
  - Schlaf
  - aktuelle VO2max
- **Services:** `DailyCoachService`, `DailyCoachContextService`
- **Persistierung:** `DailyCoachSession` pro Tag/User
- **PRO-Feature**
- **Endpoints:** `GET /api/ai-trainer/context`, `GET /api/ai-trainer`, `POST /api/ai-trainer/execute`

### 7.3 Cycle-Adaptive Training Suggestions

- LLM schlägt eine **Trainingsalternative** vor, angepasst an Zyklusphase und Wohlbefinden
- Ablauf:
  1. geplantes Training des Tages holen
  2. LLM mit Zyklus + Wellbeing anfragen
  3. Original + AI-Alternative + Erklärung zurückgeben
  4. Fallback (AI aus oder Fehler): nur Original anzeigen
- **Service:** `CycleAdaptiveTrainingService`
- **Endpoint:** `GET /api/cycle-tracking/adaptive-suggestion`

### 7.4 AI-Plan-Generierung

- Komplette Wochen-Trainingspläne per LLM generieren lassen
- **Input:** Athletenprofil (VO2max, Erfahrung, Ziele), Wettkampf, aktuelle Fitness
- **Pipeline:**
  - `PromptBuilder` → LLM → `AIPlanResponseParser` → `AIPlanValidator` → `AIPlanPersistenceService`
- **Entities:** `AiTrainingPlan`, `AiTrainingDay`, `AiTrainingWorkout`
- **Status:** `GENERATED`, `ACCEPTED`, `REJECTED`
- **Endpoints:** `POST /api/ai-training-plan/generate`, `GET /latest`, `PUT /{id}/status`
- **PRO-Feature**

---

## 8. User-Management, Auth und Sicherheit

### 8.1 Authentifizierung

- **Framework:** Spring Security + JWT
- **Token:**
  - Access Token (JWT, 24 h Gültigkeit)
  - Refresh Token (DB-basiert, mit Expiry)
  - Token-Blacklist beim Logout (SHA-256-Hash)
- **Rollen:** `USER`, `TRAINER`, `ADMIN`
- **Endpoints:** `/api/auth/register`, `/login`, `/logout`, `/refresh`

### 8.2 E-Mail-Verifikation

- Registration → Versand eines 6-stelligen Codes (Gültigkeit ~60 min)
- Auto-Resend + Pre-Fill per Link `?email=...&code=...`
- Verify-Seite rendert keine Sidebar (verhindert ungewollte `/me`-Requests)
- Unverifizierte User werden nach Login-Versuch auf die Verify-Seite geleitet

### 8.3 Passwort-Reset

- `POST /api/auth/forgot-password` erzeugt SHA-256-gehashten Token (32 Byte, 60 min)
- E-Mail mit Link `/new-password?token=...`
- `POST /api/auth/reset-password` setzt neues Passwort, revoked alle Refresh-Tokens
- **Enumeration-Schutz:** Endpoint antwortet immer 200, unabhängig davon ob die E-Mail existiert
- DB-Felder: `password_reset_token_hash`, `password_reset_token_expires_at`

### 8.4 Profil und Einstellungen

- **Profilfelder:** firstName, lastName, dateOfBirth, heightCm, weightKg, maxHeartRate, hrRest, gender, profileImage, `discoverable_by_others`
- **Settings-Seiten:**
  - **App Preferences:** Sprache, Theme (Light/Dark)
  - **Body Data:** Zyklus-Tracking, Asthma-Tracking aktivieren
  - **Community:** Community Routes, Group Events, Discoverable
  - **Integrations:** Strava (OAuth), COROS (OAuth), Garmin-Schlaf-CSV-Import
  - **Email Notifications:** Reminder-Präferenzen
- **Profile-Completion-Card** im Dashboard (benötigt mindestens Größe, Gewicht, Max-HF, Geburtstag)
- **Standort:** `latitude`, `longitude`, `location_updated_at` über Map-Picker oder Geolocation

### 8.5 Admin-Userverwaltung und DSGVO-Löschung

- **Two-Step-Bestätigung:** Admin muss Benutzername zur Bestätigung erneut eintippen
- **UserDeletionService:** native SQL, transactional, FK-sicher
- **Gelöscht werden:** Trainings, Metriken, Integrations-Tokens, Feedback, Freundschaften, Competitions, AI-Pläne, Zyklus/Asthma/Schlaf-Daten
- **Erhalten bleiben:** Community Routes (Creator-ID wird auf NULL gesetzt)
- **Audit-Log:** `AuditAction.USER_DELETED`, Actor-ID anonymisiert
- **Endpoint:** `DELETE /api/users/{id}` mit `{ "confirmUsername": "..." }`

### 8.6 Sicherheit und Compliance

- Rollen- und Ownership-Checks auf allen user-bezogenen Endpoints (IDOR-Schutz)
- Privilege-Escalation-Schutz
- CORS-Konfiguration in `config/`
- Globaler `GlobalExceptionHandler` – gibt generische Fehlermeldungen zurück
- Rate Limiting (u.a. bei Registrierung) über `RateLimitingService`
- Passwort-Mindestlänge 10 Zeichen, Spring `PasswordEncoder`
- Produktions-Secrets über `.env`-Datei und `application-prod.properties` ohne Defaults

---

## 9. PRO Subscription System

- **Enum:** `SubscriptionPlan` (`FREE`, `PRO`)
- **Feld:** `subscription_plan` auf `users`
- **Enforcement:** AOP-Annotation `@RequiresSubscription(SubscriptionPlan.PRO)` via `SubscriptionAspect`
- **Gesperrte Features (PRO):**
  - AI Trainer / Daily Coach
  - AI-Plan-Generierung
  - Group Events / Trainer Events
- **Frontend:** `ProOverlay`-Komponente blendet gesperrte Bereiche aus und leitet auf `/elite-upgrade`

---

## 10. Integrationen

### 10.1 Strava

- OAuth2-Autorisierung
- **User-scoped Tokens:** `strava_token.user_id` NOT NULL + Unique (nach Security-Hardening)
- Import historischer Aktivitäten nach Datumsbereich
- Extrahiert Distanz, Zeit, HF, Power, Cadence, Höhenmeter
- **Service:** `StravaService`
- Nur eigene Tokens können getrennt werden

### 10.2 COROS

- **API-Version:** V2.0.6
- OAuth2 + Webhook (`POST /api/coros/webhook`) für Push neuer Aktivitäten
- Sport-Mapping: 50+ Workout-Typen (Running, Cycling, Swimming, Triathlon, …)
- Profil-Sync (Nickname, Profilbild)
- Automatische Konvertierung zu `CompletedTraining` mit Dedup über `labelId`
- Token-Auto-Refresh vor Ablauf
- Audit-Logging bei Connect/Disconnect
- **Services:** `CorosService`, `CorosWebhookService`

### 10.3 Bioweather (DWD)

- Quelle: Deutscher Wetterdienst – biowetterliche Vorhersagen (Deutschland)
- **Inhalte:** Pollenwarnung, Biowetter-Stress, Luftqualität (PM2.5, Ozon)
- **Service:** `DwdWeatherService`
- User-Feld: `dwd_region`
- Kontext für Daily Coach und Cycle-Adaptive Suggestions

### 10.4 Open-Meteo

- Aktuelle Wetterdaten und Forecasts
- Luftqualität
- Kontext für Trainingsempfehlungen

### 10.5 Email (SMTP)

- Gmail-SMTP für Verifikation, Passwort-Reset, Reminder, News, Feedback-Benachrichtigungen
- **Service:** `EmailService`

---

## 11. Internationalisierung (i18n)

- **Framework:** `@ngx-translate/core` + `@ngx-translate/http-loader`
- **Sprachen:** Englisch (`en`) und Deutsch (`de`)
- **Dateien:** `frontend/src/assets/i18n/en.json`, `de.json`
- Jeder neue Text wird **zwingend** zuerst in `en.json` angelegt und in `de.json` gespiegelt
- Verwendung im Template: `{{ 'key' | translate }}`
- **Sprach-Switch:** Settings → App Preferences, gespeichert in `localStorage['pacr-language']`
- Default: Deutsch

---

## 12. Design System – KINETIC

- **Codename:** KINETIC
- **Framework:** Angular Material (Azure-Blue-Theme) + umfangreiches Custom-SCSS
- **Brand-Akzentfarbe:** `#8ffc2e` (PACR-Grün)
- **Icons:** Google Material Symbols Outlined – konsistent in der gesamten App
- **Typografie:** Fonts werden **global** eingebunden, niemals per `@import` in Komponenten-SCSS
- **Styling-Prinzipien:**
  - CSS-Variablen + Mixins in `styles.scss`
  - Zentrale Styles statt Duplikation pro Komponente
  - Exakte Farbwerte 1:1 umsetzen (keine Interpretationen)
- **Theme:** Light/Dark über `data-theme` auf `body`, gespeichert in `user.theme` bzw. `localStorage['pacr-theme']`
- **Prototyping:** erfolgt in Stitch
- **Karten:** Tiles passen sich Theme an (CARTO light_all / dark_all)

---

## 13. Admin-Bereich

Eigene Admin-Shell (`admin.routes.ts`) mit allen Verwaltungs-Funktionen. **Pattern:** kein `MatDialog`, sondern dedizierte Seiten – `news-list` dient als Styling-Template.

**Admin-Funktionen:**
- **Overview / Dashboard:** KPIs und Stats (`AdminStatsService`)
- **Plans / Trainings:** CRUD für Trainingsplan-Templates und einzelne Trainings
- **Competitions:** Verwaltung aller Wettkämpfe
- **Users:** Liste, Inline-Edit, DSGVO-konforme Löschung
- **Achievements:** `AchievementDefinition` anlegen/editieren, mit Zeitfenstern
- **News / Announcements:** `AppNews` erstellen, veröffentlichen, E-Mail an alle User
- **Login Messages:** `LoginMessage`-Management – Info-Popups nach Login, einmalig pro User quittierbar (`LoginMessageSeenLog`)
- **Feedback:** Review von `UserFeedback`, Status-Filter, Inline-Edit von Status und Notes
- **Community Routes:** GPX-Upload für kuratierte Strecken
- **Bot Runners:** CRUD für Bot-Profile + Run-Now
- **Audit Log:** `AuditLogService` protokolliert relevante Aktionen

---

## 14. Achievements

- **Entities:** `AchievementDefinition`, `UserAchievement`, `AchievementCategory`
- Unterstützt zeitlich begrenzte Awards (Start/Ende)
- **Service:** `AchievementEvaluationService` – triggert nach jedem abgeschlossenen Training
- Admin-Panel für komplette Verwaltung

---

## 15. User Feedback

- **Entity:** `UserFeedback` mit Status `OPEN`, `ACKNOWLEDGED`, `RESOLVED`, `CLOSED`
- **Kategorien:** Bug Report, Feature Request, General Feedback
- **Frontend:** Floating Action Button (unten rechts) öffnet Dialog
- **Admin:** Feedback-Tab mit Status-Filter und Inline-Bearbeitung
- **Endpoints:** `POST /api/feedback`, `GET/PUT /api/admin/feedback`

---

## 16. Weitere Features

### 16.1 About-Dialog

- Versionsnummer über `VersionController`
- Changelog über `ChangelogController` (liest `CHANGELOG.md` als Classpath-Ressource)
- Ausklappbarer Haftungsausschluss:
  - Hinweis zur Trainingsplan-Nutzung
  - Gesundheitsrisiken
  - Erklärung der FIT-Datenverarbeitung

### 16.2 Geocoding und Map-Picker

- **Service:** `GeocodingService` (Nominatim) + `ReverseGeocodingService`
- **Komponente:** `LocationPickerDialogComponent` (Leaflet-Kartendialog)
- Verwendung: Group Events, Community Routes, User-Standort, Trainer-Events

### 16.3 News / Announcements

- **Entities:** `AppNews`, `AppNewsSentLog`
- Admin veröffentlicht News, werden per E-Mail an alle User verschickt
- Anzeige als Benachrichtigungen im Frontend

### 16.4 Login Messages

- **Entities:** `LoginMessage`, `LoginMessageSeenLog`
- Info-Popup direkt nach Login, pro User einmalig
- Admin-Workflow: Create / Edit / Publish / Unpublish / Delete
- Voll i18n-fähig

### 16.5 Training Reminders

- **Services:** `TrainingReminderService`, `TrainingReminderScheduler`
- Tägliche Erinnerungen per E-Mail, konfigurierbar in den Notification-Preferences

### 16.6 Versionierung und Changelog

- Version in `backend/pom.xml`, gespiegelt in `frontend/package.json`
- Vor jedem Commit: `./version-bump.sh <patch|minor|major>` + Update `CHANGELOG.md`
- **Changelog-Format:** Keep a Changelog, Englisch, Kategorien `Added`, `Changed`, `Fixed`, `Removed`
- **Public Endpoints:** `GET /api/version`, `GET /api/changelog`

### 16.7 PWA

- Service Worker mit Offline-Caching
  - App-Shell (Static Assets)
  - API-Calls (Network-First, 1-Tag-Cache, 5 s Timeout)
- Web App Manifest (Icons 72–512 px)
- iOS-PWA-Meta-Tags und Touch Icon
- Google Fonts für Offline-Nutzung gecacht
- nginx-Konfiguration mit passenden Cache-Headern

---

## 17. Datenmodell – Übersicht

**Größenordnung:**
- ~69 Entity-Klassen (~4.860 Zeilen Java)
- 46 REST-Controller
- 71+ Services
- 351+ Java-Dateien im Backend
- 99+ Liquibase-Changelogs (Stand April 2026)

**Kerntabellen:**
`users`, `competitions`, `training_plans`, `trainings`, `user_training_entries`,
`completed_trainings`, `activity_streams`, `activity_metrics`, `daily_metrics`,
`body_metrics`, `body_measurements`, `blood_pressure`, `sleep_data`,
`cycle_entries`, `cycle_settings`, `asthma_entries`,
`personal_records`, `audit_logs`, `app_news`, `user_notification_preferences`,
`friendships`, `community_routes`, `route_attempts`,
`group_events`, `group_event_registrations`, `group_event_exceptions`,
`ai_training_plans`, `ai_training_days`, `ai_training_workouts`,
`login_messages`, `user_feedback`, `achievements`, `user_achievements`,
`bot_profiles`, `strava_tokens`, `coros_tokens`,
`blacklisted_tokens`, `refresh_tokens`.

**Migrations-Regeln (kritisch):**
- Datenbankänderungen **ausschließlich** via Liquibase – **kein** `ddl-auto=update`
- Namensschema: `NNN-beschreibung.xml` unter `backend/src/main/resources/db/changelog/changes/`
- Neue Includes immer in `db.changelog-master.xml` eintragen
- `<preConditions onFail="MARK_RAN">` muss **vor** `<comment>` stehen (sonst Startup-Crash)

---

## 18. Frontend-Routen (Übersicht)

### 18.1 User-Routen (`app.routes.ts`)

```
/login, /signup, /forgot-password, /verify-email, /new-password
/onboarding                           (protected)
/                                     (Landing Redirect)

/dashboard                            (protected)
/activities, /activities/:id          (protected)
/upload                               (protected)
/training-plans, /training-plans/:id  (protected)
/competitions                         (protected)
/statistics                           (protected)
/achievements                         (protected)

/community-routes                     (protected)
/community-routes/mine                (protected)
/community-routes/share/:activityId   (protected)
/community-routes/:id                 (protected)

/community/groups                     (protected)
/community/groups/:id                 (protected)
/community/friends                    (protected)

/trainer/events                       (protected, Trainer)
/trainer/events/create                (protected, Trainer)
/trainer/events/:id                   (protected, Trainer)
/trainer/events/:id/edit              (protected, Trainer)

/elite-upgrade                        (protected)
/settings                             (protected)

/body-data/cycle-tracking             (protected)
/body-data/log-symptoms               (protected)
/body-data/body-metrics               (protected)
/body-data/cycle-settings             (protected)
/body-data/log-body-metrics           (protected)
/body-data/asthma-tracking            (protected)

/ai-trainer                           (protected, PRO)
/admin/**                             (protected, Admin)
```

### 18.2 Admin-Routen (`admin.routes.ts`)

```
/admin/overview
/admin/plans
/admin/plans/:planId/trainings
/admin/competitions
/admin/users
/admin/achievements
/admin/news
/admin/login-messages
/admin/feedback
/admin/community-routes
/admin/bot-runners
```

---

## 19. Feature-Zusammenfassung

| Bereich | Kernfunktionen |
|---|---|
| **Training** | Wettkampf-basierte Planung, Template-Pläne, Wochenkalender, FIT/GPX/TCX-Upload, Plan-Adaptation |
| **Analytics** | VO2max (Daniels/VDOT), Daily Metrics, Strain, TRIMP, ACWR, Readiness Score, Race-Time-Predictions |
| **Gesundheit** | BodyMetrics, Gewicht/KFA, Blutdruck, Schlaf, Zyklus-Tracking, Asthma-Tracking |
| **Community** | Freunde (inkl. Nearby), Community Routes + Leaderboards, Trainer Events mit RRULE, Bot Runner |
| **AI/LLM** | Daily Coach, AI-Plan-Generierung, Cycle-Adaptive Suggestions |
| **Integrationen** | Strava OAuth, COROS OAuth + Webhook, DWD-Biowetter, Open-Meteo, Garmin FIT SDK, SMTP |
| **User Management** | Registrierung, E-Mail-Verifikation, Passwort-Reset, Login Messages, DSGVO-konforme Löschung |
| **Admin** | Dashboard, User-Management, News, Achievements, Bot-Runner, Feedback, Audit-Log |
| **UX** | i18n (en/de), Light/Dark Theme, PWA, responsives Material Design, Google Material Symbols |
| **Security** | JWT + Refresh Tokens, Rollen-/Ownership-Checks, IDOR-Schutz, Rate Limiting, globaler Exception-Handler |
| **Business** | FREE/PRO Subscription via `@RequiresSubscription` AOP-Annotation und `ProOverlay` |

---

## 20. Wichtige Entwicklungs-Konventionen (für spätere Arbeit an PACR)

- **Frontend-Verzeichnis:** `frontend/` (nicht mehr `angular-frontend/`)
- **Datums-Handling:** immer Java-/TypeScript-Datumsklassen, niemals String-Manipulation (Timezone-Bugs!)
- **Styles:** zentral in `styles.scss`, nicht pro Komponente duplizieren
- **Fonts:** global einbinden, keine `@import` in Komponenten-SCSS
- **Farben:** angegebene Werte exakt umsetzen, keine eigenen Interpretationen
- **i18n:** jeder neue Text erst in `en.json`, dann in `de.json`
- **Datenbank:** ausschließlich Liquibase – nächste Migration nach aktuell letzter Nummer
- **Version:** vor jedem Commit bumpen und Changelog pflegen
- **Exception-Handling:** `GlobalExceptionHandler` – keine `e.getMessage()` an Clients zurückgeben

---

*Ende der PACR-Wissensdatenbank.*
