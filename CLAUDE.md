# SplitEasy

Offline-first Android expense splitter with one-tap UPI settle-up. Portfolio project built by one entry-level engineer; code should be clean, idiomatic and easy to explain in an interview.

## Source of truth
- Requirements: `docs/PRD.md`. Build **P0 + P1 only**. P2 (export, reminders, recurring expenses) is out of scope.
- UI: `docs/designs/` — look at `png/` for layout, read the `.html` for exact colours, spacing and copy. Tokens are in `docs/designs/README.md`.
- If the PRD and designs disagree, the PRD wins; ask me before inventing features.

## Stack
Kotlin · Jetpack Compose + Material 3 · Navigation Compose · MVVM + domain use cases · Hilt · Coroutines/Flow · Room · WorkManager · Firebase Auth + Firestore · ML Kit Text Recognition · Jetpack Glance · JUnit, MockK, Turbine · GitHub Actions. minSdk 26. Gradle version catalog (`libs.versions.toml`).

## Architecture rules
- Packages: `ui/` (screens + ViewModels), `domain/` (pure Kotlin use cases, no Android imports), `data/` (Room, repositories, Firestore), `sync/` (WorkManager), `di/`.
- Room is the single source of truth. Repositories write to Room first, mark `syncState = PENDING`, and enqueue sync. The UI only observes Room Flows.
- Money is always `Long` paise. Never Float/Double. Rounding remainders go to the payer.
- Settlements from a UPI SUCCESS result are `PENDING` until the payee confirms.
- Records use client-generated UUIDs and `updatedAt`; deletes are soft deletes; conflicts are last-write-wins.
- No secrets in the repo: `google-services.json` stays git-ignored.

## Workflow
- Work one PRD milestone at a time (see "Milestones and timeline"). Propose a plan before large changes.
- Every change to split or settlement logic needs unit tests, including rounding edge cases.
- Before saying a task is done, run `./gradlew test lint` and fix failures.
- Small, focused commits with clear messages.
