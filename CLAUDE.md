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

**Scheduled jobs** (`@EnableScheduling`): refresh-token cleanup daily 03:00 (`RefreshTokenService`), "on track" status emails Tuesdays 17:00 (`NotificationService` → `MicrosoftService`), monthly pace-team-round creation (`PaceTeamRoundService`).

**Startup import.** `DukAppServiceApplication`'s constructor calls `UserImportService.importUsersFromCsv()`, which seeds users/teams/round 1 from a CSV only when the user table is empty. The CSV paths in `UserImportService` / `UserFamilyImportService` are hard-coded Windows paths; the source files live in `src/main/resources/imports/`.

**External integrations.** `service/microsoft/MicrosoftService` uses Graph client-credentials to write activities to SharePoint lists (paid/unpaid jobs) and send mail (status updates, new passwords). `FileStorageService` stores uploads under `./uploads`. Excel generation uses Apache POI / fastexcel with templates in `resources/imports/docs`.
