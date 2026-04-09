# Changelog

All notable changes to PACR will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Admin: GPX upload for community routes**: new admin page under `/admin/community-routes` lets admins upload curated running routes from GPX files. New endpoints `GET/POST/DELETE /api/admin/community-routes` (`AdminCommunityRouteController`, secured with `@PreAuthorize("hasRole('ADMIN')")`). `CommunityRouteService.createFromGpx` reuses `GpxParsingService` and stores the parsed track points as `gps_track_json`. `ParsedActivityData` now exposes the parsed `latLngPoints` list. Liquibase migration `095-community-route-source-activity-nullable.xml` makes `community_routes.source_activity_id` nullable so admin-uploaded routes can exist without a source `CompletedTraining`. Frontend: new standalone `AdminCommunityRoutes` component with file-upload form and list/delete actions, linked from the admin shell nav.
- **Admin email notification on new user registration**: When a new user registers, all users with the `ADMIN` role now receive a plain-text email with the new user's username, email, registration timestamp, ID and status. Implemented via new `EmailService.sendAdminNewUserNotification(...)` and a new `UserRepository.findByRole(...)` query. Failures of the admin notification do not break the registration flow.
- **AI-driven adaptive training suggestion in cycle tracking view**: The "PACR Adaptive Adjustment" card in the cycle tracking view is no longer hardcoded. It now fetches today's actually planned training and asks the LLM (via `LLMClientService`) for an alternative workout adapted to the user's current cycle phase and daily wellbeing (energy, sleep, mood, symptoms). New backend endpoint `GET /api/cycle-tracking/adaptive-suggestion` (`CycleAdaptiveTrainingController` + `CycleAdaptiveTrainingService`, DTO `AdaptiveSuggestionDto`). When AI is disabled or the LLM call fails, the original planned training is still shown without an AI explanation.
- **Forgot password / password reset**: complete end-to-end flow. New endpoints `POST /api/auth/forgot-password` and `POST /api/auth/reset-password` with secure SHA-256 hashed reset tokens (32 bytes, 60 min expiry). `EmailService.sendPasswordResetEmail` sends a link to `/new-password?token=...`. Frontend `forgot-password`, `forgot-password-confirmation` and `new-password` components are now wired to the API; on successful reset all refresh tokens for the user are revoked. Liquibase migration `093-add-password-reset-token.xml` adds `password_reset_token_hash` and `password_reset_token_expires_at` columns to `users`. Forgot-password endpoint always returns 200 to prevent user enumeration.
- **Nearby friends discovery**: the Friends "Find" tab now supports searching for other discoverable users in your vicinity via a new "Nearby" mode with selectable radius (10/25/50/100 km). Users can set their location in settings either via the map picker or the browser's current position (`PUT /api/users/me/location`, `DELETE /api/users/me/location`). New endpoint `GET /api/friendships/search/nearby?lat=&lon=&radiusKm=` returns results with Haversine-calculated distances. Liquibase migration `092-add-user-location-fields.xml` adds `latitude`, `longitude`, and `location_updated_at` columns to `users`.
- **Friends / Connections** feature in the Community menu: search for other discoverable users, send/accept friend requests, view a feed of recent activities from connected users. New backend module (`Friendship` entity, `FriendshipService`, `/api/friendships` endpoints) and new Angular `Friends` component with tabs for Activity, Friends, Requests and Find. Liquibase migration `091-add-friendships.xml` creates the `friendships` table.
- Settings: merged "Community Routes" and "Group Events" into a single **Community** card
- New toggle "Discoverable by other runners" in the Community settings card, backed by a new `discoverable_by_others` flag on the user (Liquibase migration `090`)

### Fixed
- **Security: Strava integration is now user-scoped.** Previously, `StravaToken` queries used a global `findFirstByOrderByIdAsc()` lookup, so any authenticated user saw the first connected Strava account as "Verbunden", could sync that user's activities, and could disconnect it for everyone. `StravaService` now resolves tokens via `findByUser(currentUser)`, `exchangeCodeForToken` stores the `user_id` on the token, and `disconnect` only removes the current user's token. Liquibase migration `094-strava-token-user-scope.xml` deletes orphaned tokens, marks `strava_token.user_id` NOT NULL and adds a unique constraint on it.
- Verification email link no longer falls back to `localhost:4200`. `EmailService` now fails fast at startup if `app.frontend-url` is not configured, preventing broken links in production
- `app.frontend-url` is set to `https://pacr.app` in `application-prod.properties` (no env var indirection)

### Fixed
- Verify-email page no longer renders the sidebar, which previously triggered an unauthenticated `/me` request and bounced new users to the login screen
- Registration flow: login with an unverified email now automatically redirects to the verify-email screen instead of only showing an error
- Verification email now contains a direct link (`/verify-email?email=…&code=…`) that pre-fills the code and auto-submits verification
- Verify-email screen accepts `email` and `code` via query parameters as a fallback when router state is missing

### Added
- New backend property `app.frontend-url` (env `APP_FRONTEND_URL`) used to build links in outgoing emails
- `EmailService.sendVerificationEmail(to, code, resend)` centralises the verification email content
- Login error response for unverified users now includes the `email` field so the frontend can navigate

### Added
- Admin Login Messages: admins can create info messages shown as one-time dialog after user login
- New admin tab "Login Messages" with create/edit/publish/unpublish/delete functionality
- User-facing dialog shows pending messages after login with dismiss tracking per user
- New Liquibase migration 088: `login_messages` and `login_message_seen_log` tables
- New endpoints: `GET/POST/PUT/DELETE /api/admin/login-messages`, `GET /api/login-messages/pending`, `POST /api/login-messages/{id}/dismiss`
- i18n support (en + de) for all login message labels

## [0.11.0]

### Added
- Recurring group events with full RRULE support (RFC 5545): daily, weekly, bi-weekly, monthly (Nth weekday), yearly
- Recurrence UI in trainer event form: frequency, interval, weekday chips, monthly position selector, series end date
- Dynamic RRULE expansion: recurring events are expanded on-the-fly for API responses without pre-generating instances
- Per-occurrence registration: users can register for specific dates of a recurring event independently
- Occurrence exceptions: trainers can cancel individual dates of a recurring series
- New `RecurrenceService` for RRULE parsing and date expansion using `java.time` APIs
- New `group_event_exceptions` table for cancelled occurrences (Liquibase migration 087)
- `occurrence_date` column on `group_event_registrations` for per-occurrence tracking
- New trainer endpoints: `PUT /cancel-occurrence`, `GET /occurrences`
- i18n support (en + de) for all recurrence-related labels

### Changed
- `GroupEventDto` extended with `rrule`, `recurrenceEndDate`, `occurrenceDate`, `isRecurring` fields
- Registration and cancel-registration endpoints now accept optional `occurrenceDate` query parameter
- Upcoming and nearby event queries now include expanded recurring event occurrences

### Previous (unreleased)
- Pace range (from/to) for group events: trainers can specify target pace in mm:ss/km format when creating events
- Pace filter in group events overview: users can enter their pace to find matching events
- Pace display on event cards and detail view
- New Liquibase migration (086) for pace columns on group_events table
- i18n support (en + de) for all pace-related labels

## [0.9.0] - 2026-04-03

### Added
- Community Groups / Group Events feature: users can discover and register for group runs, training sessions, and events
- New `group_events` and `group_event_registrations` database tables with Liquibase migrations (083-085)
- GroupEvent entity with full lifecycle: DRAFT → PUBLISHED → CANCELLED / COMPLETED
- User-facing endpoints: browse nearby/upcoming events, register/unregister, view registrations (`/api/group-events/*`)
- Trainer-facing endpoints: create, edit, publish, cancel, delete events, view participants (`/api/trainer/events/*`)
- `groupEventsEnabled` user setting toggle in Settings page
- Community sidebar menu restructured as expandable submenu with "Routes" and "Groups" sub-items
- New Trainer menu item (top-level) visible for TRAINER and ADMIN roles
- Trainer guard for frontend route protection
- Group Events browse screen with tabs (Near Me, All Upcoming, My Events), geolocation, and radius filter
- Group Event detail screen with registration/unregistration functionality
- Trainer Events dashboard for managing events with status badges and quick actions
- Trainer Event form for creating and editing events with all fields (date, time, location, distance, capacity, cost, difficulty)
- Trainer Event detail view with participant list
- Full i18n support (en + de) for all new screens (~60 translation keys)
- Group Events added to PRO feature set
- TRAINER role activated (previously defined but unused)

## [0.8.0] - 2026-04-02

### Added
- Full internationalization (i18n) support using @ngx-translate/core with runtime language switching
- German (de) and English (en) translation files with ~600 keys covering all 42+ components
- Language switcher in Settings under "App Preferences" — default language is German
- Language preference persisted in localStorage under `pacr-language`

### Changed
- All hardcoded UI strings in templates and TypeScript files replaced with translation keys
- Every standalone component updated to import TranslateModule

## [0.7.0] - 2026-04-01

### Added
- Progressive Web App (PWA) support: app can be installed on mobile and desktop devices
- Service worker with offline caching for app shell and static assets
- Network-first caching for API calls with offline fallback (1-day cache, 5s timeout)
- Web app manifest with PACR branding, custom icons in all required sizes (72-512px)
- Apple touch icon and iOS PWA meta tags for home screen installation
- Google Fonts cached for offline use
- nginx configuration for proper service worker and manifest cache headers

## [0.6.0] - 2026-04-01

### Added
- Backend: `POST /api/auth/logout` endpoint that blacklists the current JWT token server-side
- Token blacklist system: `BlacklistedToken` entity, repository, and `TokenBlacklistService` with SHA-256 hashing
- Scheduled cleanup of expired blacklisted tokens (hourly)
- Liquibase migration 081: `blacklisted_tokens` table with index on `token_hash`

### Fixed
- Critical session bug: logging out as User 1 and logging in as User 2 still showed User 1's data — `UserService.currentUser` signal was not cleared on logout
- JWT tokens now invalidated server-side on logout — previously tokens remained valid for 24h after logout
- `JwtAuthenticationFilter` now checks token blacklist before granting access
- Frontend logout now fully resets all cached state (user profile, theme)

## [0.5.0] - 2026-04-01

### Added
- User feedback system: floating action button (bottom-right) opens a dialog to submit bug reports, feature requests, and general feedback
- Admin feedback management: new "Feedback" tab in admin panel with status filtering, expandable detail rows, and inline status/notes editing
- Backend: UserFeedback entity, REST endpoints for users (POST /api/feedback) and admins (GET/PUT /api/admin/feedback)
- Liquibase migration 080: user_feedback table with indexes on user_id, status, and created_at

## [0.4.7] - 2026-04-01

### Added
- Liability disclaimer section in the About dialog with expandable toggle, covering training plan usage, health risks, and FIT data processing

## [0.4.6] - 2026-03-31

### Fixed
- Activity map route line not visible: added SVG glow polyline underneath hotline canvas for guaranteed visibility
- Activity map now uses light tiles (CARTO light_all) in light theme instead of always using dark tiles
- Route gradient palette adapts to theme (teal/purple/red for light, green/yellow/red for dark)
- Route outline color provides contrast on both themes (white on dark, black on light)
- Map tiles and route update live on theme switch

## [0.4.5] - 2026-03-31

### Added
- Readiness Score info dialog on dashboard: explains score zones (excellent/good/moderate/low) with color-coded ranges and practical tips

## [0.4.4] - 2026-03-31

### Added
- Strain info dialog on dashboard: explains 21-day training load zones with color-coded ranges (low/moderate/high/very high)

## [0.4.3] - 2026-03-31

### Added
- ACWR info dialog on dashboard: explains training load zones with color-coded ranges and practical tips

## [0.4.2] - 2026-03-31

### Changed
- Dashboard training load chart now shows current calendar week (Mon–Sun) instead of last 7 days
- Bar heights represent daily distance (km) with a 50 km default scale
- Day labels centered below each bar

### Added
- Stylish glassmorphism hover tooltip on load chart bars showing distance (km) and strain
- Daily distance (km) added to load trend API response (LoadTrendPointDto)

## [0.4.1] - 2026-03-30

### Added
- COROS integration in frontend settings: connect/disconnect COROS account via OAuth

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
