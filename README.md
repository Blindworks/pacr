# Smart Trainingsplan

An intelligent training plan web application that combines static training plans with real-time athlete health data to deliver AI-powered, dynamically adapted race preparation.

> The core idea: your body data drives your training — not the other way around.

## What Makes It Special

Traditional training apps give you a fixed plan and expect you to follow it. **Smart Trainingsplan** fuses structured competition preparation with live physiological data — asthma levels, VO2max, heart rate metrics, sleep quality, and bioweather conditions — to continuously adapt workouts to how the athlete is actually feeling. If conditions are bad or health metrics are off, the plan adjusts. Training becomes a dialogue between the plan and the person.

## Features

### Core Training
- **Competition Management** — Create competitions with a target race date; training schedules are automatically anchored backwards from that date
- **Template-Based Training Plans** — Upload reusable JSON training templates; the system maps them to real dates relative to each competition
- **Calendar Overview** — Week-by-week training calendar showing planned vs. completed workouts across all active competitions
- **Multi-Format Activity Import** — Upload `.fit` (Garmin SDK), `.gpx`, and `.tcx` files to capture completed workout data (heart rate, pace, power, cadence, elevation)
- **Activity Streams** — Detailed per-second activity data (HR, pace, cadence, power, altitude, GPS) with interactive multi-axis charts and toggleable data layers
- **Personal Records** — Track PRs per distance with goal times; historical PR tracking integrated with race predictions

### AI Features (optional, toggleable)
- **AI-Powered Daily Coach** — Context-aware daily training recommendations using LLM, incorporating current metrics, cycle phase, bioweather, sleep, and training load
- **AI Training Plan Generation** — Automatic weekly training plan generation based on athlete profile, competition goals, and current fitness level

### Health & Body Tracking
- **VO2max & Performance Metrics** — Automatic VO2max calculation using the Daniels/VDOT formula, with heart rate-corrected variant for more accurate real-world tracking
- **Body Measurements** — Longitudinal tracking of weight, body fat, and other body composition metrics with trend visualization
- **Blood Pressure Tracking** — Log and monitor blood pressure readings over time
- **Sleep Data Tracking** — Track sleep duration, quality, and REM percentage; bulk import from Garmin CSV
- **Asthma & Health Tracking** — Log asthma symptoms and severity; feeds directly into training intensity recommendations (can be enabled/disabled per user)
- **Menstrual Cycle Tracking** — Full cycle-phase awareness (menstrual, follicular, ovulation, luteal) with daily symptom logging (mood, energy, sleep quality, flow intensity, physical symptoms); phase-specific training guidance (optional, can be enabled/disabled)

### Performance Modelling
- **Training Load & Readiness** — ACWR (Acute:Chronic Workload Ratio) with 7-day acute and 28-day chronic load, Strain, TRIMP, aerobic decoupling, and a composite Readiness Score (0–100) with color-coded risk flags (BLUE/GREEN/ORANGE/RED)
- **Training Impact Simulation** — Predict strain impact before starting a workout; simulate full-week load with recovery-aware projections and injury risk forecasting
- **Race Time Predictions** — Context-aware race pace predictions (1km, 5km, 10km, Half-marathon, Marathon) combining VO2max, weekly mileage, long-run distance, and ACWR fatigue factor

### External Integrations
- **Strava Integration** — OAuth2 connection to import activities, sync metrics, and disconnect; parses activity streams (power, HR, pace, elevation)
- **Bioweather Integration** — Pulls bioclimatic forecast data from Germany's DWD (Deutscher Wetterdienst) including pollen alerts, bioweather stress levels, air quality (PM2.5, ozone), and current weather via Open-Meteo
- **Email Reminders** — Configurable daily training reminder emails via SMTP; user-managed notification preferences

### Administration
- **Admin Dashboard** — System-wide KPIs (users, competitions, trainings), audit log with filtering and pagination, user management view
- **Audit Logging** — Automatic logging of all significant actions (login, CRUD operations, status changes) with async persistence
- **App News** — Admin-published announcements delivered to all users
- **Role-Based Access** — ADMIN and USER roles with `@PreAuthorize`-protected endpoints and frontend route guards

### User Experience
- **JWT Authentication** — Secure user account system; each athlete's data is isolated and personalized
- **Two-Step Signup** — Guided registration with profile completion
- **User Profile Validation** — Dashboard requires complete profile (max HR, weight, etc.) before showing data
- **Statistics View** — Training intensity distribution, VO2max trends, body measurement trends, fitness trend graphs with time-range filters
- **User Image Upload** — Profile picture support with avatar display in navigation

## Technology Stack

### Backend
- **Java 21** + **Spring Boot 3.2**
- **MariaDB** — production database
- **Liquibase** — schema migrations (no `ddl-auto=update`)
- **Spring Security + JWT** — authentication & authorization with role-based access
- **JPA / Hibernate** — ORM
- **Garmin FIT SDK 21.176.0** — local Maven dependency for `.fit` file parsing
- **OpenAI / Claude API** — optional AI features (toggleable via `pacr.ai.enabled`)
- **DWD Open Data API** — bioweather/bioclimate forecast
- **Open-Meteo API** — weather and air quality data
- **SMTP (Gmail)** — email notifications and reminders

### Frontend
- **Angular 19** — standalone components architecture
- **Angular Material** (Azure Blue theme) — UI component library
- **TypeScript** + **RxJS** — reactive data layer
- Dev server on `localhost:4200`

### Deployment
- **Docker** + **docker-compose** — containerized backend, frontend, and MariaDB networking
- Environment-based configuration with volume mounting for uploads

### Architecture Highlights

**Training as Template:** `Training` entities are reusable templates containing `weekNumber` + `dayOfWeek` (no hardcoded dates). The `UserTrainingEntry` table maps each athlete's competition registration to concrete training dates, calculated backward from the race date. One plan template serves many athletes across different competition schedules.

**Health-Driven Adaptation:** Body metrics, sleep data, asthma logs, cycle phase, and bioweather data are evaluated alongside planned training load. The system uses this combined signal to recommend intensity adjustments before the athlete even starts a session.

**Conditional AI Features:** AI capabilities (Daily Coach, Plan Generation) are loaded conditionally via `@ConditionalOnProperty`. When disabled, fallback controllers provide graceful degradation.

**Advanced Load Modelling:** The system computes daily metrics including Strain, TRIMP, ACWR, aerobic decoupling, and a composite Readiness Score. These feed into training impact predictions and injury risk forecasting.

## Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- MariaDB Server
- Maven 3.8+

### Database Setup
```sql
CREATE DATABASE smart_trainingsplan;
CREATE USER 'smart_trainingsplan'@'localhost' IDENTIFIED BY 'taxcRH51#';
GRANT ALL PRIVILEGES ON smart_trainingsplan.* TO 'smart_trainingsplan'@'localhost';
FLUSH PRIVILEGES;
```

### Backend
```bash
cd backend
mvn spring-boot:run
```
Runs on `http://localhost:8080`

> **Note:** The Garmin FIT SDK is a local Maven dependency. See `MAVEN_SETUP.md` for installation instructions before building.

### Angular Frontend
```bash
cd angular-frontend
npm install
npm start
```
Runs on `http://localhost:4200`

### Docker (optional)
```bash
docker-compose up
```
Backend on port `8081`, Frontend on port `4201`.

## Training Plan JSON Format

Training plans are uploaded as JSON templates. Week and day references are relative (not absolute dates), so one plan can be reused for any competition:

```json
{
  "trainings": [
    {
      "name": "Interval Training",
      "description": "5x1000m intervals with 3min rest",
      "weekNumber": 8,
      "dayOfWeek": "TUESDAY",
      "type": "speed",
      "intensity": "high",
      "startTime": "18:00",
      "duration": 90
    },
    {
      "name": "Easy Long Run",
      "description": "Relaxed 15km base run",
      "weekNumber": 8,
      "dayOfWeek": "SATURDAY",
      "type": "endurance",
      "intensity": "low",
      "duration": 90
    }
  ]
}
```

**Supported types:** `speed`, `endurance`, `strength`, `race`, `fartlek`, `recovery`, `swimming`, `cycling`, `general`

**Supported intensities:** `high`, `medium`, `low`, `recovery`, `rest`

## Key API Endpoints

### Competitions
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/competitions` | List all competitions |
| `POST` | `/api/competitions` | Create competition |
| `PUT` | `/api/competitions/{id}` | Update competition |
| `DELETE` | `/api/competitions/{id}` | Delete competition |

### Training Schedule
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/training-plans/upload` | Upload JSON training plan |
| `GET` | `/api/user-training-entries/calendar` | Calendar view (`?from=&to=`) |
| `PUT` | `/api/user-training-entries/{id}` | Update entry status/completion |

### Activities & Performance
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/completed-trainings/upload` | Upload Garmin `.fit` / `.gpx` / `.tcx` file |
| `GET` | `/api/completed-trainings/by-date` | Get actuals by date (`?date=YYYY-MM-DD`) |
| `GET` | `/api/body-metrics` | Fetch body metrics history |
| `GET` | `/api/personal-records` | List personal records |
| `POST` | `/api/personal-records` | Create/update personal record |

### Health Tracking
| Method | Path | Description |
|--------|------|-------------|
| `GET/POST` | `/api/cycle-entries` | Cycle entry CRUD |
| `GET/POST` | `/api/cycle-settings` | Cycle configuration |
| `GET` | `/api/cycle-settings/status` | Current cycle phase and progression |
| `GET/POST` | `/api/asthma-entries` | Asthma symptom logs |
| `GET/POST` | `/api/sleep-data` | Sleep data CRUD |
| `GET/POST` | `/api/blood-pressure` | Blood pressure readings |
| `GET/POST` | `/api/body-measurements` | Body composition data |

### Load & Readiness
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/daily-metrics` | Daily strain, ACWR, readiness |
| `POST` | `/api/daily-metrics/compute-today` | Recompute today's metrics |
| `POST` | `/api/training-impact/predict` | Predict workout strain impact |
| `POST` | `/api/training-impact/simulate-week` | Simulate weekly load |
| `GET` | `/api/race-predictions` | Context-aware race time predictions |

### Strava
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/strava/authorize` | Start OAuth2 flow |
| `GET` | `/api/strava/callback` | OAuth2 callback |
| `POST` | `/api/strava/import` | Import activities by date range |
| `DELETE` | `/api/strava/disconnect` | Revoke Strava access |

### AI Features
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/daily-coach` | Get today's AI coaching recommendation |
| `POST` | `/api/ai-training-plan/generate` | Generate weekly AI training plan |

### Admin (ADMIN role required)
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/admin/stats` | System-wide KPIs |
| `GET` | `/api/admin/audit-log` | Audit log with filtering |

## Database Schema (Core Tables)

| Table | Purpose |
|-------|---------|
| `users` | Athlete accounts with profile data (incl. max heart rate) |
| `competitions` | Race events with target dates |
| `training_plans` | Uploaded plan templates |
| `trainings` | Template training entries (week + day references) |
| `user_training_entries` | Athlete-specific scheduled trainings with real dates |
| `completed_trainings` | Parsed data from actual workouts (FIT/GPX/TCX) |
| `activity_streams` | Per-second activity data (HR, pace, cadence, power, altitude, GPS) |
| `daily_metrics` | Daily strain, ACWR, readiness scores |
| `body_metrics` | Longitudinal VO2max and performance measurements |
| `body_measurements` | Weight, body fat, and body composition |
| `blood_pressure` | Blood pressure readings |
| `sleep_data` | Sleep duration, quality, REM |
| `cycle_entries` | Daily cycle logs (symptoms, mood, energy, sleep, flow intensity) |
| `cycle_settings` | Per-user cycle configuration |
| `asthma_entries` | Asthma symptom and severity logs |
| `personal_records` | PRs per distance with goal times |
| `audit_logs` | System-wide action audit trail |
| `app_news` | Admin announcements |
| `user_notification_preferences` | Email reminder settings per user |

## Development

```bash
# Backend tests
cd backend && mvn test

# Frontend tests
cd angular-frontend && npm test

# Production builds
cd angular-frontend && npm run build
cd backend && mvn package
```

## License

Created for demonstration purposes.
