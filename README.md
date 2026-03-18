# Smart Trainingsplan

An intelligent training plan web application that combines static training plans with real-time athlete health data to deliver AI-powered, dynamically adapted race preparation.

> The core idea: your body data drives your training — not the other way around.

## What Makes It Special

Traditional training apps give you a fixed plan and expect you to follow it. **Smart Trainingsplan** fuses structured competition preparation with live physiological data — asthma levels, VO2max, heart rate metrics, and bioweather conditions — to continuously adapt workouts to how the athlete is actually feeling. If conditions are bad or health metrics are off, the plan adjusts. Training becomes a dialogue between the plan and the person.

## Features

- **Competition Management** — Create competitions with a target race date; training schedules are automatically anchored backwards from that date
- **Template-Based Training Plans** — Upload reusable JSON training templates; the system maps them to real dates relative to each competition
- **Garmin FIT File Integration** — Upload `.fit` files from Garmin devices to automatically capture completed workout data (heart rate, pace, power, cadence, elevation)
- **VO2max & Performance Metrics** — Automatic VO2max calculation using the Daniels/VDOT formula, with heart rate-corrected variant for more accurate real-world tracking
- **Body Metrics Dashboard** — Longitudinal tracking of training load and physiological KPIs over time
- **Asthma & Health Tracking** — Log asthma symptoms and health events; these feed directly into training intensity recommendations
- **Menstrual Cycle Tracking** — Full cycle-phase awareness (menstrual → follicular → ovulation → luteal) with daily symptom logging (mood, energy, sleep quality, flow intensity, physical symptoms); the system delivers phase-specific training guidance — e.g., peak performance work during the follicular/ovulation phases, recovery-focused sessions during the menstrual phase
- **Bioweather Integration** — Pulls bioclimatic forecast data from Germany's national weather service (DWD) to factor environmental stress into daily training readiness
- **Dynamic Plan Adaptation** — Training intensity and load automatically adjust based on current health indicators, cycle phase, completed workouts, and environmental conditions
- **JWT Authentication** — Full user account system with secure authentication; each athlete's data is isolated and personalized
- **Calendar Overview** — Week-by-week training calendar showing planned vs. completed workouts across all active competitions

## Technology Stack

### Backend
- **Java 21** + **Spring Boot 3.2**
- **MariaDB** — production database
- **Liquibase** — schema migrations (no `ddl-auto=update`)
- **Spring Security + JWT** — authentication & authorization
- **JPA / Hibernate** — ORM
- **Garmin FIT SDK 21.176.0** — local Maven dependency for `.fit` file parsing
- **DWD Open Data API** — bioweather/bioclimate forecast integration

### Frontend
- **Angular 19** — standalone components architecture
- **Angular Material** (Azure Blue theme) — UI component library
- **TypeScript** + **RxJS** — reactive data layer
- Dev server on `localhost:4200`

### Architecture Highlights

**Training as Template:** `Training` entities are reusable templates containing `weekNumber` + `dayOfWeek` (no hardcoded dates). The `UserTrainingEntry` table maps each athlete's competition registration to concrete training dates, calculated backward from the race date. This means one plan template can serve many athletes across different competition schedules.

**Health-Driven Adaptation:** Body metrics, asthma logs, and bioweather data are evaluated alongside planned training load. The system uses this combined signal to recommend intensity adjustments before the athlete even starts a session.

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

### Health & Performance
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/completed-trainings/upload` | Upload Garmin `.fit` file |
| `GET` | `/api/completed-trainings/by-date` | Get actuals by date (`?date=YYYY-MM-DD`) |
| `GET` | `/api/body-metrics` | Fetch body metrics history |
| `POST` | `/api/body-metrics` | Log new body metrics entry |

### Cycle Tracking
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/cycle-entries` | All cycle entries for current user |
| `GET` | `/api/cycle-entries/by-date` | Entry for specific date (`?date=YYYY-MM-DD`) |
| `POST` | `/api/cycle-entries` | Log new cycle entry (symptoms, mood, energy, sleep, flow) |
| `PUT` | `/api/cycle-entries/{id}` | Update cycle entry |
| `DELETE` | `/api/cycle-entries/{id}` | Delete cycle entry |
| `GET` | `/api/cycle-settings` | Get user's cycle configuration |
| `POST` | `/api/cycle-settings` | Save cycle settings (cycle length, period duration) |
| `GET` | `/api/cycle-settings/status` | Current cycle phase, day, and phase progression |

## Database Schema (Core Tables)

| Table | Purpose |
|-------|---------|
| `users` | Athlete accounts with profile data (incl. max heart rate) |
| `competitions` | Race events with target dates |
| `training_plans` | Uploaded plan templates |
| `trainings` | Template training entries (week + day references) |
| `user_training_entries` | Athlete-specific scheduled trainings with real dates |
| `completed_trainings` | Garmin FIT data from actual workouts |
| `body_metrics` | Longitudinal health & performance measurements |
| `cycle_entries` | Daily cycle logs (symptoms, mood, energy, sleep, flow intensity) |
| `cycle_settings` | Per-user cycle configuration (cycle length, period duration, last period start) |

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
