# Changelog

All notable changes to PACR will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
