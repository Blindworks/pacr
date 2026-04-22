# Changelog

All notable changes to PACR will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.22.0] - 2026-04-10

### Changed
- **Friends activity feed redesigned to match Stitch mockup**: Activity cards now use a vertical layout with prominent uppercase title, stat grid with green accent values in glass-effect boxes, squared avatars, relative timestamps ("2H AGO // RUNNING"), and date/time display. Map tile from CARTO dark basemap shown as subtle full-card background when GPS coordinates are available. Backend `FriendActivityDto` extended with `startLatitude`/`startLongitude` fields. New i18n keys for stat labels (distance, duration, pace, BPM, elevation, calories) in EN and DE.

## [Unreleased]

### Added
- **Competitions: organizer website URL.** A competition can now store an optional link to the organizer's website. Admins can set the URL in the competition create/edit form (`/admin/competitions/new`, `/admin/competitions/:id/edit`), users see it as a clickable link in the competition info dialog on `/competitions`. Backend: new nullable `organizer_url VARCHAR(500)` column on `competitions` (Liquibase migration `118-competition-organizer-url.xml`), `Competition` entity + `CompetitionDto` extended, `CompetitionService.save` merges the field on update. New i18n keys `ADMIN.ORGANIZER_URL`, `ADMIN.ORGANIZER_URL_PLACEHOLDER`, `COMPETITIONS.ORGANIZER_WEBSITE` in EN and DE.
- **Admin: manage user competition registrations.** A new admin tab `/admin/registrations` lists every `CompetitionRegistration` (user, competition, assigned training plan, format, registered-at) with search across user email/name, competition and plan. Admins can delete a single user's registration (cascading removal of the user's `UserTrainingEntry` rows and their `PlanAdjustment` rows) or delete an entire competition globally (cascading removal of ALL registrations and their training entries). Global training plan templates remain untouched. This replaces the previous need to edit the database manually to support users. Backend: new `AdminRegistrationController` (`GET /api/admin/registrations`, `DELETE /api/admin/registrations/{id}`) and `AdminCompetitionController` (`DELETE /api/admin/competitions/{id}`), both `@PreAuthorize("hasRole('ADMIN')")`. `CompetitionService.unregister` was refactored to share its cascade logic with the new `deleteRegistrationById` admin path, and a join-fetch query `findAllForAdmin` was added to `CompetitionRegistrationRepository` to avoid N+1. New `AdminRegistrationDto` exposes user, competition, plan and format details. New i18n keys under `ADMIN.REGISTRATIONS.*` and `ADMIN.TAB_REGISTRATIONS` in EN and DE.
- **Competition formats: 20K, 30K and 40K added as competition format types.** The `CompetitionType` enum now includes the intermediate road-race distances `TWENTY_K` (20K), `THIRTY_K` (30K) and `FORTY_K` (40K), inserted in the type dropdown when creating/editing a competition (`/admin/competitions/new`, `/admin/competitions/:id/edit`) and shown correctly in the admin competition list and user-facing competition view. No DB migration required (`@Enumerated(EnumType.STRING)`).
- **Admin Trainings view: export a complete training plan as JSON.** A new `Export Plan` button in the admin training list (`/admin/plans/:planId/trainings`) downloads the current state of the plan (name, description, targetTime, prerequisites, competitionType, plus all trainings grouped by `weekNumber`/`dayOfWeek` including their steps, step-blocks and prep-tips) as a pretty-printed JSON file in the v2.0 format consumed by `POST /api/training-plans/upload-template`. This enables moving locally-built plans into production by downloading + re-uploading. New admin-only endpoint `GET /api/admin/training-plans/{id}/export` (`@PreAuthorize("hasRole('ADMIN')")`) sets `Content-Disposition: attachment; filename="plan-{id}-{slug}.json"`. Implemented via new export DTOs under `com.trainingsplan.dto.export` and a new `TrainingPlanService.exportAsJson(Long)` method. New i18n keys `ADMIN.EXPORT_PLAN` / `ADMIN.EXPORT_PLAN_ERROR` in EN and DE.
- **Leave an active training plan from the Training Plans view.** Users can now exit any active plan directly from `/training-plans`. A new "Active Plans" section lists every plan the user is registered for (one entry per `CompetitionRegistration` with a training plan). Each entry has a "Leave Plan" button that opens a confirmation dialog explaining the consequences. On confirmation, the existing `DELETE /api/competitions/{id}/register` endpoint is called. The backend `CompetitionService.unregister` was extended to be transactional and to also delete all `UserTrainingEntry` rows linked to the registration plus their `PlanAdjustment` rows before removing the `CompetitionRegistration`. Competition and training plan templates as well as uploaded `CompletedTraining` (FIT) records are preserved. New `AuditAction.COMPETITION_UNREGISTERED` is recorded. New i18n keys under `TRAINING_PLAN.ACTIVE_PLANS`, `LEAVE_PLAN`, `LEAVE_PLAN_CONFIRM_TITLE`, `LEAVE_PLAN_CONFIRM_BODY`, `LEAVE_PLAN_CONFIRM` in EN and DE.

### Fixed
- **Login message form: Save/Cancel buttons still covered the user search dropdown — fixed for good by dropping `position: absolute`.** Three previous attempts (isolation, explicit `z-index: 500 vs 0`, `:has()` on the field) all failed in practice because the dropdown's stacking context and the form-actions' stacking context never reliably resolved to the same parent context across browsers. Real fix: stop floating the dropdown entirely. `.autocomplete-hint` and `.autocomplete-results` are now normal in-flow block elements inside the flex column `.autocomplete`. The dropdown simply pushes the Save/Cancel buttons down while visible — no overlap is physically possible. Removed all z-index gymnastics on `.form-actions`, `.field`, `.autocomplete`, `.autocomplete-hint`, `.autocomplete-results`. (`login-message-form.scss`)
- **Login message form: user search dropdown was covered by the Save/Cancel buttons.** The autocomplete results list on `/admin/login-messages/new` (when `targetType=USERS`) appeared behind the form action buttons. The first attempt (creating a stacking context on `.autocomplete` via `isolation: isolate`) did not help because the dropdown still had to compete in an outer stacking context that the siblings also occupied. Final fix: drop the isolation trick on `.autocomplete`, set the absolute-positioned dropdown/hint to `z-index: 500`, and explicitly place `.form-actions` at `position: relative; z-index: 0` so the two end up in the exact same stacking context with a clear ordering. Loading/no-results hint is now also absolute so it no longer pushes the buttons down while typing. (`login-message-form.scss`)

### Added
- **Admin users list: send login message shortcut.** Each row in `/admin/users` now has a mail icon button next to edit/delete that navigates to `/admin/login-messages/new?userId=…` and preselects the recipient (`targetType=USERS`, user appears as chip) — admins no longer need to search for the user in the login message form. New backend endpoint `GET /api/admin/users/{id}/summary` returns `{id, username, email}` for prefill. New i18n key `ADMIN.SEND_LOGIN_MESSAGE` (EN/DE).
- **Login messages: targeted audiences.** Admin login messages (`/admin/login-messages`) can now be targeted to a specific audience instead of always being shown to all users. The form offers three exclusive modes: `All users` (existing behavior, default), `User groups` (multi-select among `PRO`, `FREE`, `TRAINER` — driven by `User.subscriptionPlan` and `User.role`), and `Specific users` (autocomplete search by username/email with chip-based selection). Backend extends `LoginMessage` with `targetType`, `targetGroups` (`@ElementCollection`) and `targetUsers` (`@ManyToMany`); `LoginMessageService.findPendingForUser` filters published messages by the caller's group/user membership before checking the seen log. New admin endpoint `GET /api/admin/users/search?q=&limit=` powers the user autocomplete. Liquibase migration `117-login-message-targeting.xml` adds the `target_type` column (default `ALL`) and the join tables `login_message_target_groups` / `login_message_target_users`. New i18n keys under `ADMIN.LOGIN_MSG_TARGET_*` and `ADMIN.LOGIN_MSG_GROUP_*` in EN and DE.

### Changed
- **Achievement system: evaluation decoupled from achievement key.** The admin UI for achievements (`/admin/achievements`) was misleading: even though the form let admins freely pick a key, category and threshold, the backend evaluator (`AchievementEvaluationService`) selected what to measure for `PR` and `PLAN_COMPLETION` achievements via hardcoded `key.startsWith(...)` checks (`first_pr`, `pr_all_distances`, `pr_10_broken`, `week_100_pct`, `plan_completed`). Custom admin-created achievements with other keys silently never unlocked or behaved unexpectedly. A new explicit `AchievementMetric` enum now drives evaluation: `TOTAL_DISTANCE_KM`, `STREAK_DAYS`, `PR_TOTAL_COUNT`, `PR_DISTINCT_DISTANCES`, `PERFECT_WEEKS_COUNT`, `COMPLETED_PLANS_COUNT`. Each achievement carries its metric explicitly; the category (used for grouping/filter only) is auto-derived. The admin form replaces the category dropdown with a metric dropdown and shows the unit on the threshold field. `PERFECT_WEEKS_COUNT` and `COMPLETED_PLANS_COUNT` are now true counts (so admins can require e.g. "5 perfect weeks") instead of boolean flags. Liquibase migration `116-add-achievement-metric.xml` adds the `metric` column and backfills existing rows from `category` and `achievement_key`.
- **AchievementSeeder is now insert-only.** Previously the seeder rewrote name/description/icon/threshold/sortOrder for every default achievement on every startup, which silently reverted any admin edits to those fields. The seeder now only inserts achievements that don't yet exist in the database; admin edits via `/admin/achievements` are preserved across restarts.

### Fixed
- **News Hub: "Read article" button now opens a full-article modal.** Clicking any news card (hero, trending rail, or feed entry) or the `Read article` CTA now opens a modal dialog showing the hero image, topic tag, title, publication date, full article content (rendered as sanitized HTML via Angular's `DomSanitizer`), and read-only counters for views/likes/comments. Previously the click only triggered view tracking with no visible feedback. Reuses the existing `.modal-backdrop` / `.modal-panel` pattern from the comments dialog; new state signals `articleOpen`/`articleNews` and handler `closeArticle()` in `NewsHub` component.

### Changed
- **Privacy policy (DE/EN) overhauled for Garmin Connect Developer Program application.** The landing-page privacy policy templates (`frontend/public/landing/datenschutz.html`, `frontend/public/landing/en/privacy.html` and their `webpage/` duplicates) have been rewritten from a 7-section generic template into a 12-section GDPR-compliant policy that covers the full PACR application. The new structure explicitly documents: the Garmin Connect OAuth integration (data retrieved, purpose, EU storage, no-sharing commitment, disconnect flow via `Profile → Integrations → Disconnect Garmin`, and data deletion on request); other third-party integrations (Strava OAuth `activity:read_all`, COROS OAuth with webhook, OpenAI/LangChain4j with model `gpt-4o-mini`, Nominatim/OpenStreetMap, Leaflet tiles, Google Fonts, Tailwind CDN, Microsoft 365 SMTP); all data categories actually processed by PACR (account data with BCrypt-hashed passwords, training/activity data from FIT/TCX/GPX uploads, activity streams, computed metrics, community data, OAuth tokens, location data, logs, audit log); purposes and legal bases per category; EU hosting with placeholder for concrete provider; retention periods; international transfers safeguards; security measures (HTTPS, BCrypt, JWT with refresh rotation, server-side ownership checks); concrete how-to steps for users to exercise GDPR rights (account deletion via `UserDeletionService`, disconnect, subject access via info@pacr.app). Removed the amber "Final Review Note" placeholder section.
### Added
- **External running news importer: scheduled RSS/Atom feed ingestion.** A new scheduled service (`ExternalNewsImporterScheduler`, cron `0 0 9,17 * * *`) fetches configured feeds twice daily (09:00 + 17:00 server time) and imports new items as drafts into the existing News Hub. Each admin can manage feed sources at `/admin/news-sources` (new CRUD page: name, feed URL, language "de"/"en", enabled flag, manual "Fetch now" button). Imported articles are stored with the original source URL, hero image URL (referenced, not downloaded) and a stable external GUID for deduplication, land as `isPublished=false` so admins can review before publishing, and link back to the source via a "Read on {source}" CTA that opens in a new tab (`target="_blank" rel="noopener noreferrer"`). Users can multi-select the news languages they want to see under Settings; the News Hub feed is filtered by their preference (manual news with `language=null` remain visible to everyone). Backend: new entity `ExternalNewsSource`, extensions to `AppNews` (`externalGuid` UNIQUE, `externalUrl`, `externalImageUrl`, `language`, `externalSource` FK) and to `User` (`preferredNewsLanguages`). New services `RssFeedParser` (Rome library, Java `HttpClient`) and `ExternalNewsImporterService`. New admin REST endpoints under `/api/admin/news-sources` and new user endpoint `PUT /api/users/me/news-languages`. Public `GET /api/news`, `GET /api/news/featured`, `GET /api/news/trending-news` now filter by the caller's language preference. `AppNewsDto` extended with `externalUrl`, `externalImageUrl`, `sourceName`, `language`. Liquibase migrations `113-add-external-news-source.xml`, `114-extend-app-news-external.xml`, `115-add-user-news-language-prefs.xml`. New dependency `com.rometools:rome:2.1.0`. New i18n keys under `NEWS_HUB`, `ADMIN.NEWS_SOURCES_*`, `SETTINGS.NEWS_LANGUAGES_*`, and `LANGUAGE` in EN and DE.
- **News Hub: likes, inline comments and trending indicator for news posts.** Each news card now shows three counters (views, likes, comments) and a `Trending` pill overlay when the news has accumulated a configurable number of views in the last 7 days (threshold: 10). A new top-3 `Trending now` rail above the main feed surfaces the news with the highest weighted score (`views + likes*3 + comments*5` over 7 days). Clicking the heart icon toggles a like (optimistic UI with rollback), and clicking the chat icon expands an inline comment thread under the card with a compose input. Backend: new entities `AppNewsLike` and `AppNewsComment` with repositories, new endpoints `POST /api/news/{id}/like`, `GET/POST /api/news/{id}/comments`, `DELETE /api/news/comments/{commentId}` (author or admin), `GET /api/news/trending-news?limit=3`. `AppNewsDto` extended with `viewCount`, `likeCount`, `commentCount`, `hasLiked`, `isTrending`; `AppNewsCommentDto` includes a `canDelete` flag per user. Liquibase migrations `111-add-app-news-likes.xml` and `112-add-app-news-comments.xml` (unique constraint on `(news_id, user_id)` for likes). New i18n keys `NEWS_HUB.TRENDING`, `TRENDING_BADGE`, `VIEWS`, `LIKES`, `COMMENTS_LABEL`, `WRITE_COMMENT`, `POST_COMMENT_ACTION`, `NO_COMMENTS`, `COMMENTS_LOADING`, `LOGIN_TO_INTERACT` in EN and DE.

### Changed
- **News Hub social cards now render the actual route polyline** instead of a static CartoDB tile centered on the start point. Backend `FriendActivityDto` gains a nullable `previewTrack` (`double[][]`) field populated by a new helper in `FriendshipService` that loads the `ActivityStream.latlngJson`, parses it and downsamples to 60 points (same pattern as `CommunityRouteService.downsampleTrack`). Frontend `NewsHub` reuses the existing `RouteMiniMapComponent` (brand-green polyline on CartoDB tiles) as a faded card background when a track is available, and falls back to the single-tile image for activities with only a start coordinate. SCSS adds a `.map-bg--route` variant that fades the whole leaflet container (tiles + polyline) uniformly so card content stays readable.

### Added
- **News Hub**: new top-level section (sidebar entry above Dashboard, route `/news-hub`) that merges editorial announcements with friend training activity. The page has a featured hero card, a mixed feed (`News` + `Social` tabs + `All`), a right-hand sidebar with "Live Training Now" (friends whose planned training for today is not yet completed) and "Trending Topics" (aggregated from topic tags of published news in the last 30 days, sorted by view count). Backend: new public endpoints `GET /api/news`, `GET /api/news/featured`, `GET /api/news/{id}`, `POST /api/news/{id}/view` (idempotent per user), `GET /api/news/trending` (top-5 tags with headline + view/news counts), `GET /api/friendships/live-training`, and activity-level social endpoints `GET/POST /api/activities/{id}/kudos` (idempotent toggle), `GET/POST /api/activities/{id}/comments`, `DELETE /api/activities/{id}/comments/{commentId}` — all with friend-only privacy enforcement (owner OR accepted friend). New entities `ActivityKudos`, `ActivityComment`, `AppNewsView` with unique constraints preventing duplicate kudos/views; `AppNews` extended with `topicTag`, `heroImageFilename`, `isFeatured`, `excerpt`. Liquibase migrations `107`-`110`. `FriendActivityDto` extended with `activityId` so the frontend can call kudos/comments endpoints. Admin news form extended with topic tag, hero image URL, excerpt and "feature in hero" toggle. Frontend: new `PublicNewsService`, `ActivitySocialService`, `FriendshipService.getLiveTraining()`, `NewsHub` component with inline comments modal. New i18n keys `NAV.NEWS_HUB` and `NEWS_HUB.*` (EN/DE).

### Changed
- **Readiness score now considers the last 4 days of training load with time-weighted decay** (was: 1-2 days). Hard sessions from several days ago still influence today's readiness via an exponential decay window (T-1=0.80, T-2=0.55, T-3=0.30). Thresholds tuned so a hard Saturday session still lowers Tuesday's score instead of decaying to 95. Applied consistently in both `MetricsKernelService` and the legacy `ReadinessService` via a shared `ReadinessDeductionCalculator` utility. ACWR RED/ORANGE deductions adjusted to -30/-18 (base score remains 100).

### Added
- **`GET /api/daily-metrics/explain` endpoint** to diagnose the readiness score. Returns the base score, final score, recommendation, all applied deductions (with input value, threshold, source and contributing days) and all raw inputs (ACWR, daily strain for T-3..T, Z4+Z5 per day, weighted aggregates, last decoupling, sleep/HRV/body battery). Read-only — does not persist. A structured log line `metrics_kernel_readiness_deductions` is emitted on every readiness compute for production diagnosis.

### Added
- **Repeating step blocks (Nx) for trainings**: admin training editor can now group steps into repeating blocks (e.g. `5x (1000m work + 200m rest)`) instead of duplicating individual steps. New backend entity `TrainingStepBlock` with `repeatCount`, optional `label` and a nested `OneToMany` of `TrainingStep`s; `TrainingStep` gains a nullable `block` reference and `blockSortOrder`. Top-level free steps and blocks share the same `sortOrder` space, so the original order on the training is preserved. `TrainingService.findById/save/update` were extended to fetch and persist blocks together with their steps. New Liquibase migration `106-add-training-step-blocks.xml` adds the `training_step_blocks` table and the `block_id` / `block_sort_order` columns on `training_steps` (both with cascade-delete FK constraints). Frontend admin form (`/admin/trainings/:id/edit`) replaces the flat step list with a tagged-union `items` FormArray; new `[+ Block]` button creates a block with an editable `repeatCount` input, optional label and nested step list. Estimated distance, duration and calories are now computed by multiplying block contents by `repeatCount`. The user-facing training detail (`/training/:id`) renders blocks as a compact card with a green left accent border, an `Nx` badge and indented step list. New i18n keys `ADMIN.ADD_BLOCK`, `ADD_STEP_TO_BLOCK`, `REMOVE_BLOCK`, `BLOCK_REPEAT_COUNT`, `BLOCK_LABEL`, `BLOCK_LABEL_PLACEHOLDER` in EN and DE.

### Changed
- **Community routes cards now show a mini-map preview**: Each card in the `/community-routes` grid renders a small non-interactive Leaflet map of the route in the header area instead of the unused gradient placeholder. Backend `CommunityRouteDto` gains a nullable `previewTrack` (`double[][]`) field populated by a new stride-based `downsampleTrack()` helper in `CommunityRouteService` (capped at 60 points so JSON stays small). New standalone `RouteMiniMapComponent` (`frontend/src/app/components/shared/route-mini-map/`) renders the polyline in brand green `#8ffc2e` on CartoDB dark tiles, auto-fits to the route bounds, and disables all user interaction (drag/zoom/scroll). Card header height reduced from 12rem to 9rem, overlay gradient strengthened so the route name remains legible. Routes without a GPS track fall back to the original gradient placeholder.

### Added
- **Admin: rename community routes**: The `/admin/community-routes` list now supports inline editing of route names directly in the table row. Clicking the edit icon turns the name cell into a focused text input with Save/Cancel buttons; Enter saves, Escape cancels. Validation errors are shown beneath the input. New backend endpoint `PUT /api/admin/community-routes/{id}` (body `{ "name": "..." }`) via `AdminCommunityRouteController.renameRoute` and `CommunityRouteService.adminRenameRoute`, secured with `@PreAuthorize("hasRole('ADMIN')")`. Works for both admin-uploaded and user-shared routes.

### Added
- **GDPR-compliant user deletion from admin area**: Admins can now permanently delete user accounts and all associated personal data (trainings, metrics, integrations, tokens, feedback, friendships, competitions, AI plans, cycle/asthma/sleep data, etc.) directly from the admin user list. Community routes created by the deleted user are preserved and have their `creator_id` set to `NULL` so they remain available to the community. A two-step confirmation dialog requires the admin to retype the target username before the `Delete permanently` button is enabled, preventing accidental deletions. Admins cannot delete their own account. New backend service `UserDeletionService` performs all deletions in a single transaction using native SQL in FK-safe order, and anonymizes the actor id on existing audit-log entries. New endpoint `DELETE /api/users/{id}` with body `{ "confirmUsername": "..." }`, secured with `@PreAuthorize("hasRole('ADMIN')")`. New `AuditAction.USER_DELETED` is written after a successful deletion. Liquibase migration `099-community-routes-creator-nullable.xml` drops the `NOT NULL` constraint on `community_routes.creator_id`.
- **Bot Runner system**: admins can now create scheduled virtual runners that automatically pick a nearby community route and generate a realistic `CompletedTraining` + `ActivityStream` on a weekday/time schedule. Bots are ordinary `User` rows with a new `is_bot` flag (login is explicitly blocked in `AuthController` to prevent takeover) and a 1:1 `BotProfile` entity holding pace range, distance range, HR profile, home coordinates, search radius, weekday set, start time and jitter. `BotRunnerService.executeBot` selects a route via `CommunityRouteRepository.findInBoundingBox` filtered by the bot's distance range, copies the GPS track from the route (or its source activity stream), generates a plausible heart-rate series, saves the activity, and publishes `TrainingCompletedEvent` so the existing `RouteAttemptService` auto-matches the run into the leaderboard (opt-in per bot via `includeInLeaderboard`). `BotRunnerScheduler` ticks every minute (`@Scheduled(cron = "0 * * * * *")`) and runs all due bots. New admin REST API `GET/POST/PUT/DELETE /api/admin/bot-runners` and `POST /api/admin/bot-runners/{id}/run-now`, all secured with `@PreAuthorize("hasRole('ADMIN')")`. New frontend admin tab under `/admin/bot-runners` (`BotRunnerList` + `BotRunnerForm`) with full i18n. Liquibase migration `098-add-bot-runners.xml` adds `users.is_bot` and the new `bot_profiles` table.
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
