# Milestone 4 — Sign-in and sync (plan)

**Status:** approved. All questions in §3 answered (recommended option each time).

## 1. Goal

From the PRD: **FR-1** (Google sign-in, usable offline after the first sign-in) and **FR-10** (background sync with retry and backoff), plus the security requirement (Firestore rules: members only, ownership and role checks). With M4 done, all P0 requirements are complete (MVP).

**Done means:** you sign in with Google; everything you do is saved on the phone first and uploaded within a minute of being online; signing in on a second phone with the same account shows the same groups; changes on one phone appear on the other while both are open; Firestore rules block anyone who isn't a member.

## 2. What exists already

- Repositories write to Room first, mark records `PENDING` and call `SyncScheduler.requestSync()` (a no-op today).
- Every record has a client UUID, `updatedAt`, `syncState`; deletes are soft deletes; group events are append-only.
- Sync indicators exist but are hidden behind `FeatureFlags.SHOW_SYNC_STATUS`.
- Firebase project `spliteasy-aaef2` (Spark plan): Google sign-in enabled, Firestore in `asia-south1` (production mode, so everything is denied until rules are deployed), debug SHA-1 registered, `google-services.json` in `app/` and git-ignored.
- Sign-in design: `docs/designs/01-sign-in`.

## 3. Open questions

### M4-Q1. How do two different people end up in the same synced group?
- **Problem:** in M4 a group only syncs to people listed in it. Adding someone by name creates a guest, and turning a guest into a real account is the invite-link feature (FR-11), planned for M5. So in M4, sync works between **your own phones**, but not yet between two different people.
- **Options:**
  - a) M4 syncs your own groups across your devices; FR-11 invite links in M5 connect other people. The data model and rules are built for multiple users from day one, so M5 only adds the link and the "join" step. *(Recommended: keeps M4 focused; the PRD already puts FR-11 in M5)*
  - b) Move FR-11 invite links into M4 (M4 becomes about twice as big).
- **Answer:** a (own devices in M4; invite links in M5).

### M4-Q2. What happens to M2's local data at the first sign-in?
- **Agreed in M2-Q1:** it's wiped. Confirming the details:
  - a) First sign-in on a phone: delete all local Room data and the "Last used" / dismissed-prompt settings, keep the theme. *(Recommended, as agreed)*
  - b) Keep it and upload it under the new account.
- **Answer:** a (wipe at first sign-in, keep the theme).

### M4-Q3. What does Sign out do?
- **Problem:** after signing out, the next person on the phone must not see the previous account's groups.
- **Options:**
  - a) Sign out deletes local data. If any changes haven't uploaded yet, it first warns "N changes haven't synced yet. Sign out anyway?" *(Recommended)*
  - b) Sign out keeps local data until another account signs in.
- **Answer:** a (delete local data, warn about unsynced changes).

### M4-Q4. Firestore's own offline cache
- **Problem:** Firestore keeps its own offline copy by default. Room is already our offline copy, so the data would be stored twice.
- **Options:**
  - a) Turn Firestore's cache off; Room is the only local store. *(Recommended: matches "Room is the single source of truth")*
  - b) Leave it on.
- **Answer:** a (Firestore cache off; Room only).

### M4-Q5. Where does the sync code live?
- **Options:**
  - a) New modules: `:core:sync` (WorkManager worker, Firestore reads/writes, merging into Room) and `:feature:auth` (sign-in screen). *(Recommended: same pattern as M2.5; Firebase stays out of features)*
  - b) Put sync inside `:core:data` and the sign-in screen inside `:app`.
- **Answer:** a (`:core:sync` and `:feature:auth`).

### M4-Q6. `google-services.json` in CI
- **Problem:** the build needs this file, but it's git-ignored, so CI can't build once Firebase is added.
- **Options:**
  - a) Store it as a GitHub Actions secret; CI writes it to `app/` before building. You'd add the secret (I'll give exact steps). *(Recommended)*
  - b) Commit it. It isn't truly secret (it ships inside every APK), but `CLAUDE.md` says to keep it out of the repo.
- **Answer:** a (GitHub Actions secret).

### M4-Q7. Firestore security rules: tests and deployment
- **Options for testing:**
  - a) Test the rules against the Firebase emulator in CI (a small Node test suite, ~1–2 min extra per run). *(Recommended: rules are security-critical and easy to get wrong)*
  - b) Test them by hand in the Firebase console's rules playground.
- **Options for deploying:**
  - a) Rules live in the repo (`firebase/firestore.rules`); you deploy with `firebase deploy --only firestore:rules` after installing the Firebase CLI. *(Recommended)*
  - b) You paste them into the Firebase console.
- **Answer:** a (emulator tests in CI; rules deployed from the repo).

### M4-Q8. How fresh do other devices' changes need to be?
- **Options:**
  - a) While the app is open, listen to your active groups live (changes appear within seconds). When the app opens, fetch what changed since the last sync. Archived groups are fetched once, not listened to. *(Recommended: matches the PRD's "listen only to active groups")*
  - b) Only fetch when the app opens or on pull-to-refresh (cheaper, less live).
- **Answer:** a (live listeners on active groups + fetch on start).

### M4-Q9. The debug "Load demo data" button
- **Problem:** after sign-in, demo groups would upload to your Firestore account.
- **Options:**
  - a) Keep it (debug builds only); the demo groups sync like any other. Useful for testing sync. *(Recommended)*
  - b) Remove it in M4.
- **Answer:** a (keep it, debug only).

## 4. Design

### Firestore layout
```
users/{uid}                         displayName, createdAt
groups/{groupId}                    name, createdBy, memberUserIds[], isArchived,
                                    lastActivityAt, updatedAt, isDeleted
groups/{groupId}/members/{id}       displayName, userId?, upiId?, createdAt, updatedAt, isDeleted
groups/{groupId}/expenses/{id}      all expense fields + shares {memberId: {paise, basisPoints?}}
groups/{groupId}/settlements/{id}   all settlement fields
groups/{groupId}/events/{id}        append-only guest UPI ID changes
```
- `memberUserIds` on the group lets a device ask "which groups am I in?" with one query, and lets the rules check membership cheaply.
- Your UPI ID is stored on your member row in each group (as now). Changing it in Profile updates every group you're in, in one batch.

### Upload (FR-10)
- `SyncScheduler` becomes a WorkManager job: unique (`sync`), needs a network connection, exponential backoff starting at 10 s.
- The worker reads all `PENDING` rows, writes them to Firestore in batches of up to 500, then marks each row `SYNCED`, **but only if its `updatedAt` hasn't changed meanwhile**. An edit made during the upload stays `PENDING` and goes up on the next run.
- A guest UPI ID change and its group event go in the same batch, as the rules require (PRD sync strategy).

### Download and conflicts
- On app start: fetch documents changed since the last sync time (per group); then listen live to active groups while the app is in the foreground (M4-Q8).
- Merge rule (last write wins): a remote record replaces the local one if the remote `updatedAt` is newer, **unless the local row is still `PENDING` and newer**. Deletes arrive as `isDeleted = true` like any other change.
- `updatedAt` comes from the phone's clock, as the PRD specifies. A phone with a badly wrong clock could win conflicts it shouldn't; this is listed as a known limitation.

### Security rules (summary)
- Only signed-in users who are in `memberUserIds` can read a group and its subcollections.
- Members can add and edit expenses; only the expense's creator can edit or delete it (Q11).
- Settlements: the payer creates them; only the payee confirms or rejects; status changes follow M1's rules.
- Member rows: only the owner changes their own `upiId`; a guest's `upiId` can be changed by any member only if the same batch writes a matching event (`getAfter()`).
- Only the creator archives the group or removes members; events can be created but never changed or deleted.
- "Balance must be zero" checks stay in the app (Firestore rules can't add up balances); listed as a known limitation in the README.

### Sign-in flow
- App start: no Firebase user → Sign in screen (01). Signed in → Groups home, even offline (Firebase remembers the user).
- "Continue with Google" uses Android's Credential Manager with Sign in with Google, then Firebase Auth.
- `CurrentUserProvider` returns the Firebase user (uid, Google name); the placeholder "You" goes away.
- Profile shows your Google name and photo initials, the sync card ("All changes synced" / "N changes waiting"), and **Sign out**.
- `FeatureFlags.SHOW_SYNC_STATUS` is turned on (offline banner, "Not synced" labels).

### Libraries (checked against compile SDK 36)
| Library | Version | Why |
| --- | --- | --- |
| Firebase BoM | 35.0.2 | Firebase Auth + Firestore |
| `androidx.credentials` + `credentials-play-services-auth` | 1.6.0 | Credential Manager (newest stable) |
| `googleid` | 1.2.1 | Sign in with Google option |
| WorkManager | 2.12.0 | Sync worker |
| `androidx.hilt:hilt-work` | 1.3.0 | Hilt-injected worker |
| `google-services` Gradle plugin | 4.5.0 | Reads `google-services.json` |

All of these work with compile SDK 36, so no SDK upgrade is needed.

## 5. Steps (one PR each)

1. **This plan** (docs only).
2. **Firebase setup:** libraries, `google-services` plugin, CI secret (M4-Q6). App still behaves as in M2.
3. **Sign-in:** `:feature:auth`, sign-in screen (01), Firebase `CurrentUserProvider`, wipe on first sign-in (M4-Q2), Sign out (M4-Q3).
4. **Firestore mapping:** `:core:sync` with Firestore document ↔ entity mapping and a `RemoteDataSource` interface (so sync logic is testable without Firebase).
5. **Upload:** WorkManager worker, real `SyncScheduler`, batching, mark-SYNCED-if-unchanged.
6. **Download:** fetch-on-start, live listeners on active groups, last-write-wins merge into Room.
7. **Security rules:** `firebase/firestore.rules`, emulator tests in CI (M4-Q7), you deploy them.
8. **Sync status and wrap-up:** turn on sync indicators, Profile sync card, two-device test by hand, PRD update, M4 outcome.

## 6. Tests

- Unit tests with a fake `RemoteDataSource`: upload batching, the mark-SYNCED race, last-write-wins merge (newer remote, newer pending local, deletes, events), first-sign-in wipe.
- WorkManager's test helpers for the worker's constraints and retries.
- Rules tests against the Firestore emulator: allowed and denied cases for every rule above.
- Compose UI tests for the sign-in screen and Profile's sign-out warning.
- Manual: two phones (or a phone and an emulator) on the same account; airplane mode on, make changes, airplane mode off, check they arrive within a minute.

## 7. What you'll need to do

- Answer §3.
- Add the `google-services.json` secret to GitHub (if M4-Q6 a).
- Install the Firebase CLI and run one deploy command for the rules (if M4-Q7 a).
- Run the two-device test at the end (I can't run emulators here).
