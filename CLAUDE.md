# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Backend (Spring Boot + Maven)
```bash
cd backend
mvn spring-boot:run          # Run in development mode (port 8080)
mvn test                     # Run all tests
mvn test -Dtest=ClassName    # Run a single test class
mvn package                  # Build production jar
```

### Angular Frontend (Active)
```bash
cd angular-frontend
npm install
npm start                    # Dev server on port 4200
npm test                     # Run unit tests with Karma
npm run build                # Production build
```

### Database Setup (MariaDB)
```sql
CREATE DATABASE smart_trainingsplan;
CREATE USER 'smart_trainingsplan'@'localhost' IDENTIFIED BY 'taxcRH51#';
GRANT ALL PRIVILEGES ON smart_trainingsplan.* TO 'smart_trainingsplan'@'localhost';
FLUSH PRIVILEGES;
```

Backend credentials are in `backend/src/main/resources/application.properties`. H2 in-memory DB is used for tests.

## Architecture

### Technology Stack
- **Backend**: Java 21, Spring Boot 3.2.0, JPA/Hibernate, MariaDB, Garmin FIT SDK 21.176.0
- **Angular Frontend**: Angular 19, Angular Material (Azure Blue theme), TypeScript, RxJS — **this is the active frontend**
- **React Frontend** (`frontend/`): Legacy, kept for reference only

### Backend Package Structure (`com.trainingsplan`)
- `controller/` — REST endpoints (one controller per entity)
- `entity/` — JPA entities: `Competition`, `TrainingPlan`, `TrainingWeek`, `Training`, `TrainingDescription`, `CompletedTraining`
- `service/` — Business logic; `TrainingCompletionService` handles planned-vs-actual comparison
- `repository/` — Spring Data JPA repositories
- `dto/` — Data transfer objects; `DailyTrainingCompletionDto` aggregates daily stats
- `config/` — CORS and Spring configuration

### Entity Relationships
```
Competition → TrainingPlan → TrainingWeek → Training
                                              ↓
                                   TrainingDescription (optional rich text)

CompletedTraining (from FIT file upload, linked to date — NOT a FK to Training)
```
`CompletedTraining` is matched to planned `Training` by date only, not by a foreign key.

### Angular Frontend (`angular-frontend/src/app/`)
- **Routing** (`app.routes.ts`): `/competitions` → list, `/competitions/new|:id/edit` → form, `/competitions/:id/upload` → plan upload, `/overview` → calendar, `/completion` → FIT upload
- **API Service** (`services/api.service.ts`): Single injectable service wrapping all backend calls; base URL from `environments/environment.ts` (`http://localhost:8080/api`)
- **Models** (`models/competition.model.ts`): All TypeScript interfaces in one file; `Training` has both new fields (`trainingDate`, `intensityLevel`, `trainingType`) and legacy fields (`date`, `intensity`, `type`) for compatibility
- **Components** (all standalone): `TrainingPlanOverviewComponent` is the main calendar view with week navigation and multi-competition display; `TrainingDetailsDialogComponent` is a Material dialog for per-training details

### Key API Endpoints
- `POST /api/competitions/{id}/generate-weeks` — triggers automatic week generation after competition creation
- `POST /api/training-plans/upload` — multipart upload of JSON training plan file
- `POST /api/completed-trainings/upload` — multipart upload of `.fit` file (parsed by Garmin SDK)
- `GET /api/completed-trainings/by-date?date=YYYY-MM-DD` — fetch actual training data by date
- `PUT /api/trainings/{id}/feedback` — update completion status and rating

### FIT File Processing
The Garmin FIT SDK (`com.garmin:fit:21.176.0`) is a local dependency. It must be installed to the local Maven repository — see `MAVEN_SETUP.md`. `CompletedTrainingService` parses uploaded `.fit` files and stores metrics (HR, pace, power, cadence, elevation) in `CompletedTraining`.

### Version Management
- Current version: `0.1.0` (defined in `backend/pom.xml`, mirrored in `frontend/package.json`)
- **Before every commit**, bump the version using `./version-bump.sh <patch|minor|major>`:
  - `patch` — bugfixes, small corrections
  - `minor` — new features, new endpoints, new pages
  - `major` — breaking changes, large redesigns
- Include the bumped `pom.xml` and `package.json` in the same commit
- The version is exposed via `GET /api/version` (public) and shown in the frontend About dialog

### Training Plan JSON Upload Format
```json
{
  "trainings": [
    {
      "name": "Intervalltraining",
      "description": "5x1000m Intervalle mit 3min Pause",
      "date": "2024-01-15",
      "type": "speed|endurance|strength|race|fartlek|recovery|swimming|cycling|general",
      "intensity": "high|medium|low|recovery|rest",
      "startTime": "18:00",
      "duration": 90
    }
  ]
}
```
