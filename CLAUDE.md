# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 4 / Java 21 REST backend for DukApp (package `com.ktk.dukappservice`). Uses PostgreSQL via JPA, JWT auth, and Microsoft Graph (SharePoint lists + mail). There is no `src/test` directory yet.

## Commands

```bash
./mvnw spring-boot:run          # run locally (port ${PORT:8990}, context path /dukapp)
./mvnw clean package            # build jar into target/
./mvnw test                     # run tests
./mvnw test -Dtest=ClassName#methodName   # run a single test
```

All endpoints are served under `/dukapp/api/...` (context path + controller mapping).

## Configuration

`src/main/resources/application.properties` holds everything: the active datasource points at a LAN PostgreSQL server (`10.10.2.31`), with a commented-out localhost profile. Schema is managed by `spring.jpa.hibernate.ddl-auto=update` — there are no migrations, so entity changes alter the live schema on startup. The file also contains JWT secrets, Microsoft Graph app IDs/SharePoint list IDs (bound to `config/MicrosoftConfig`), and the Google client ID.

## Architecture

**Layering.** `controllers/` → domain services in `data/<entity>/` → Spring Data repositories. Each `data/<entity>/` package holds the JPA entity, its `*Repository`, and its `*Service`. Services extend `service/BaseService<E, ID>` (generic CRUD over `getRepository()`); entities extend `data/BaseEntity` (identity `id`). Custom queries live on repositories as `fetchByQuery(...)` methods with nullable filter params; `data/ServiceUtils` parses date-range strings for them.

**DTOs.** Controllers accept/return DTOs from `dto/`. Conversion is done either with hand-written mappers in `mapper/` (extending `BaseMapper`, which forces ModelMapper `STRICT` matching) or with the shared `ModelMapper` bean (defined in `controllers/RestConfiguration`) directly inside controllers. `spring-boot-starter-data-rest` is also on the classpath and `RestConfiguration` exposes IDs for all entities.

**Security.** `security/DukAppSecurityConfig` is stateless JWT: `JwtAuthenticationFilter` reads `Authorization: Bearer`, `DukAppDetailsManager` loads users. Public routes: `/api/auth/**`, GET `/api/donations`, `/api/payments`. Refresh tokens are persisted (`RefreshToken*`). A separate `BookingJwtUtils` issues tokens for an external booking system (`/api/auth/bookingToken`). Passwords use a `DelegatingPasswordEncoder` with bcrypt for new hashes and a legacy `{sha256}` / plain-match fallback for older stored passwords — keep that fallback intact unless migrating users. Many controllers additionally do role checks manually by taking a `userId` request param and rejecting `Role.USER` (roles: `ADMIN`, `USER`, `TEAM_LEADER`, `HELPER`). CORS allows `https://dukapp.bcc-ktk.org` and `http://localhost:8999`.

**Domain: points/credit tracking.** Users belong to teams and seasons divided into rounds. Transactions (`transactions`, `transactionitems`) record credits per user/round; after any change the derived stats must be recalculated via `service/TransactionServiceUtils` (`updateUserStatus` → `UserStatusService` + `PaceUserRoundService`; `calculateAllTeamStatus` → `PaceTeamRoundService`). Controllers that mutate transactions/items call these explicitly — do the same for new write paths.

**Domain: jobs (pre-registration).** A `Job` (`data/jobs/`, `JobController` at `/api/job`) is an activity that has not happened yet: people register for it, and afterwards the responsible user submits hours per registered user (`POST /api/job/{id}/complete`). Completing creates a normal `Activity` + `ActivityItem`s (not yet registered in the app), which then goes through the existing `/api/activity/{id}/register` flow, so that flow is untouched. All rules live in `JobService`, which throws `ResponseStatusException` (400 invalid, 403 not allowed, 404, 409 wrong state/full/deadline, 422 not eligible); `JobController` renders them as plain-text bodies.
- *Capacity and waitlist:* `maxParticipants` (null = unlimited) and `waitlistEnabled`. A `JobRegistration` is `REGISTERED`, `WAITLISTED` or `CANCELLED` (rows are kept; re-registering reuses the row and goes to the back of the waitlist). A freed place goes to the oldest waitlisted user. Everything that changes who holds a place runs in a transaction under a pessimistic lock on the job row (`JobRepository.findByIdForUpdate`).
- *Deadlines:* no registrations after `registrationDeadline`; registered users can only cancel until `cancellationDeadline` (waitlisted users can always leave; `JOB_MANAGE_ALL` can always cancel).
- *Eligibility:* optional `minAge`/`maxAge` (inclusive, measured on the job day) and `genderRestriction` (`User.gender`, new nullable column; users without it can't join gender-restricted jobs). Enforced for everyone, admins included.
- *On behalf of others:* a user may register/cancel themselves, their children (same `familyId`, actor older than 18, child 18 or younger, same boundary as `UserController#getFamily`), or anyone with `JOB_MANAGE_ALL`. Comments on registrations are only returned to the registrant, their parents, the registrar and the job's organizers.
- *Permissions:* `JOB_CREATE` (create jobs, edit/cancel your own), `JOB_MANAGE_ALL` (any job, any registration). The responsible user can always complete their own job. Existing roles are not updated by `initializeRoles()`, so assign these to roles via the roles API.
- *Tests:* `src/test/.../JobServiceTest` (Mockito, in-memory registration store) covers capacity, waitlist, deadlines, eligibility, family rules and completion.

**Notifications.** Push goes through Firebase Cloud Messaging (`service/notifications/PushService`; set `app.firebase.credentialsFile` / `FIREBASE_CREDENTIALS_FILE` to a service account JSON, otherwise pushes are only logged). Clients register their FCM token via `POST /api/notifications/devices` (and delete it on logout). `PushNotificationService` decides recipients and sends in the background — call it from controllers *after* the transactional service call returned: new job (eligible users of `app.users.baseChurch`), job cancelled (registered + waitlisted), registered by someone else, manually created transaction items (one push per user per request). Users can switch each `NotificationType` off (`/api/notifications/preferences`; no row = enabled), including the `ON_TRACK_EMAIL`. Admins send one-off pushes (`/api/notifications/general`, `NOTIFICATION_SEND`) and manage weekly ones plus the on-track e-mail's day/time (`/api/notifications/schedules`, `NOTIFICATION_SCHEDULE_MANAGE`); `NotificationScheduler` checks them every minute. Push texts are Hungarian (`NotificationTexts`).

**Scheduled jobs** (`@EnableScheduling`): refresh-token cleanup daily 03:00 (`RefreshTokenService`), notification schedules every minute (`NotificationScheduler`; the "on track" status e-mail defaults to Tuesday 17:00, `NotificationService` → `MicrosoftService`), monthly pace-team-round creation (`PaceTeamRoundService`).

**Startup import.** `DukAppServiceApplication`'s constructor calls `UserImportService.importUsersFromCsv()`, which seeds users/teams/round 1 from a CSV only when the user table is empty. The CSV paths in `UserImportService` / `UserFamilyImportService` are hard-coded Windows paths; the source files live in `src/main/resources/imports/`.

**External integrations.** `service/microsoft/MicrosoftService` uses Graph client-credentials to write activities to SharePoint lists (paid/unpaid jobs) and send mail (status updates, new passwords). `FileStorageService` stores uploads under `./uploads`. Excel generation uses Apache POI / fastexcel with templates in `resources/imports/docs`.
