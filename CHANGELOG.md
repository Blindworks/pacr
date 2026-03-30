# Changelog

All notable changes to PACR will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.4.0] - 2026-03-29

### Added
- Community Routes feature: share running routes from completed activities with GPS data
- Route discovery: browse public community routes nearby with configurable search radius
- Route leaderboards: per-route rankings by total time, filterable by All Time / This Month / This Week
- Route attempts: select a route before running, auto-assign synced activity with distance plausibility check (+/-30%)
- Community Routes opt-in toggle in user profile settings (privacy-first, disabled by default)
- New sidebar navigation item for Community Routes (visible when feature is enabled)
- Share as Community Route button on activity detail page (for activities with GPS data)
- My Shared Routes management page for editing or unsharing routes
- Backend: CommunityRoute and RouteAttempt entities with Haversine-based nearby search
- Backend: CommunityRouteController and RouteAttemptController REST APIs
- Backend: Automatic route attempt assignment via TrainingCompletedEvent listener

## [0.3.0] - 2026-03-29

### Added
- COROS API integration (V2.0.6) with OAuth 2.0 authorization flow
- COROS webhook endpoint for receiving workout data push (`POST /api/coros/webhook`)
- COROS service status check endpoint (`GET /api/coros/status`)
- COROS sport type mapper supporting 50+ workout types (running, cycling, swimming, triathlon, etc.)
- Automatic conversion of COROS workout data to CompletedTraining entities with dedup via labelId
- COROS user profile sync (nickname, profile photo) on connection
- COROS token management with automatic refresh before expiry
- Audit logging for COROS connect/disconnect events

## [0.2.0] - 2026-03-29

### Added
- Blood Pressure Trends chart on Body Measures page (systolic & diastolic)
- Resting Heart Rate chart on Body Measures page
- Reusable chart template for all body metrics graphs (eliminates code duplication)
- Changelog feature with backend endpoint and About dialog integration

### Changed
- Detail metric cards (Resting HR, Blood Pressure, etc.) moved above charts for better visibility
- Chart tooltip now flips left when near right edge to prevent clipping

## [0.1.1] - 2025-03-29

### Added
- Body metrics graph visualization
- User profile editing (first name, last name, password change)
- Version numbering system with About dialog
- Auto-adapt training plan feature with missed workout detection and readiness-based adjustments
- Production deployment setup with Docker Compose and .env config
- Cycle Sync section on landing pages (DE and EN)
- Audit logging system

### Changed
- Auto-generated competitions are filtered out when no competition is selected
- Initial user is now created automatically on fresh deployment
- Landing page text and icon improvements (SVG icons replace emojis)

### Fixed
- Audit logging expanded and corrected
- Landing page layout corrections
- VO2 Max gauge label overflow for long classification text
- Session invalidation on logout

## [0.1.0] - 2025-03-01

### Added
- 5-step onboarding wizard for new users
- Achievement and badge system with time-bound support
- Admin area for achievement management
- GPS map view for completed activities with fullscreen dialog
- Strava integration for activity sync
- Admin dashboard with user email settings and reminders
- AI Trainer feature
- Training plan overview with calendar view
- FIT file upload and parsing (Garmin SDK)
- Competition management with training plan generation
- JWT authentication with Spring Security
- Liquibase database migrations
- VO2max calculation (Daniels/VDOT formula) with heart rate correction
- Body metrics tracking
- User training entry system (template-based training plans)

### Fixed
- Duplicate Pace/HF toggle hidden in fullscreen map dialog
- Map tile brightness improved for better visibility
- Training completion event publishing from Strava service
- Dashboard error when user height/weight/birthdate not entered
- Hardcoded server addresses removed
