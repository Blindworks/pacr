# Repository Guidelines

## Project Structure & Module Organization
- `angular/`: Angular 21 client (Standalone Components + Material). Core app code is in `angular/src/app/{components,services,models}`; global styles in `angular/src/styles.scss`.
- `backend/`: Spring Boot API (`com.trainingsplan`). Follow the layered layout: `controller/`, `service/`, `repository/`, `entity/`, `dto/` under `backend/src/main/java/com/trainingsplan`.
- Database migrations live in `backend/src/main/resources/db/changelog/changes` and are wired via `db.changelog-master.xml`.
- Example plan payloads are stored at repository root as `*_training.json`.

## Build, Test, and Development Commands
- Frontend setup/run:
```bash
cd angular
npm install
npm start
```
Starts Angular dev server on `http://localhost:4200`.
- Frontend test/build:
```bash
npm test
npm run build
```
Runs Karma/Jasmine tests and creates production build output.
- Backend run/test/package:
```bash
cd backend
mvn spring-boot:run
mvn test
mvn package
```
Starts API (default `http://localhost:8080`), executes tests, and builds the JAR.

## Coding Style & Naming Conventions
- Java: 4-space indentation, `PascalCase` classes, `camelCase` members, one top-level class per file.
- TypeScript/Angular: 2-space indentation; keep Angular naming conventions (`feature-name.component.ts`, `.html`, `.scss`, `.spec.ts`).
- Prefer small service methods and thin controllers/components; put reusable API calls in `services/api.service.ts`.
- No dedicated formatter config is committed; keep style consistent with surrounding files before submitting.

## Testing Guidelines
- Frontend uses Jasmine + Karma. Place specs beside implementation files as `*.spec.ts`.
- Backend uses Spring Boot Test/JUnit via Maven (`mvn test`). Add tests under `backend/src/test/java` when introducing backend logic.
- No enforced coverage threshold is configured; at minimum, cover new logic paths and failure handling.

## Commit & Pull Request Guidelines
- Commit subjects in this repo are short, imperative, and capitalized (e.g., `Add Strava integration`, `Fix FIT file upload parameter mismatch`).
- Keep commits focused by feature/fix; avoid mixing frontend/backend refactors unless tightly coupled.
- PRs should include: purpose summary, key changes, manual test steps/commands run, linked issue (if any), and UI screenshots for visible frontend updates.

## Security & Configuration Tips
- Never commit real credentials. Use local `application.properties` overrides/environment variables for secrets.
- Validate Liquibase changes against a local DB before merging schema updates.
