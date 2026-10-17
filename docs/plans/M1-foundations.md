# Milestone 1 — Foundations: plan

**Status:** Approved. All M1 questions answered "recommended".
**PRD phase:** Foundations, weeks 1–2: "Project setup, Room schema, split logic + tests".
**Sources:** `docs/PRD.md` (updated after review), `docs/OPEN_QUESTIONS.md`, `CLAUDE.md`.

---

## 1. Scope

**In M1**
- The Android project: Gradle with a version catalog, Hilt, an app class and a placeholder screen, git and `.gitignore`.
- Quality tooling: ktlint, Android lint, a coverage gate, and GitHub Actions CI.
- The domain layer (pure Kotlin): money formatting and parsing, split calculation, balances, settlement simplification, and the settlement, group and expense rules from the review.
- The data layer: Room entities, type converters, database v1 with an exported schema, DAOs and DAO tests.

**Not in M1** (later milestones)
- Screens, theme, fonts and navigation, plus the repositories that write to Room and queue a sync: M2.
- Firebase Auth, Firestore, the sync worker, security rules and `google-services.json.example`: M4.
- Invite links, receipt OCR, widget and charts: M5. README, demo GIF and the signed APK: M6.

---

## 2. Open questions that block M1

These come up when the PRD data model and the review answers are turned into an actual Room schema and domain rules. The recommended answers are suggestions only.

### M1-Q1. How is a settlement soft-deleted? — affects the schema
- **Problem:** Q6 (delete a confirmed settlement) and F4 (cancel your pending payment) both say "soft delete", but `SettlementEntity` has no `isDeleted`, and Q10 limited the new fields to a specific list.
- **Options:**
  - a) Add `SettlementEntity.isDeleted`, the same as every other synced table. *(Recommended)*
  - b) Add a `CANCELLED` status instead of a flag.
- **Answer:** a (recommended).

### M1-Q2. What does "member order" mean? — affects rounding
- **Problem:** Q7 gives the leftover paise to "the first participant in member order" when the payer isn't in the split, but no field defines member order. The order must be the same on every device.
- **Options:**
  - a) Add `MemberEntity.createdAt` and use join order, breaking ties by member ID. The same order is used everywhere members are listed (you first in the UI). *(Recommended)*
  - b) Alphabetical by display name, with no new field (but renaming someone changes the order).
- **Answer:** a (recommended).

### M1-Q3. Editing a percentage split later
- **Problem:** Only `sharePaise` is stored, so the original percentages can't always be recovered. For example, ₹1 at 33.33 / 33.33 / 33.34 % stores 33 / 33 / 34 paise, which reopens as 33 / 33 / 34 %.
- **Options:**
  - a) Add a nullable `ExpenseShareEntity.percentBasisPoints`, filled in only for percentage splits, so editing shows exactly what was entered. *(Recommended)*
  - b) No new field; when editing, show percentages worked out from the amounts, which may differ slightly.
- **Answer:** a (recommended).

### M1-Q4. UPI or cash: which confirmation message to show
- **Problem:** Section G has different messages for UPI and cash payments. Using "has a `upiTxnId`" to tell them apart isn't reliable, because some UPI apps report SUCCESS without a transaction ID.
- **Options:**
  - a) Add `SettlementEntity.method` (`UPI` or `CASH`). *(Recommended)*
  - b) Work it out from whether `upiTxnId` is filled in.
- **Answer:** a (recommended).

### M1-Q5. Order of expenses on the same day
- **Problem:** The feed is newest first by `date`, which has no time. The only other timestamp is `updatedAt`, so editing an expense would move it to the top of its day.
- **Options:**
  - a) Add `ExpenseEntity.createdAt` and sort by `date`, then `createdAt`, newest first. *(Recommended)*
  - b) Sort by `date`, then `updatedAt`, so edited expenses move to the top.
- **Answer:** a (recommended).

### M1-Q6. Who can delete a settlement between two guests?
- **Problem:** Under F3, any member can record a payment between two guests. Under Q6, only the payer or payee can delete one. When both sides are guests, nobody could ever delete it.
- **Options:**
  - a) Any member can delete it, the same people who could record it. *(Recommended)*
  - b) Only the member who recorded it, which would need a new `createdBy` field on settlements.
- **Answer:** a (recommended).

### M1-Q7. Pending payments and "balance must be zero"
- **Problem:** A pending payment doesn't change balances (Q4), so a member could have a zero balance while a payment to or from them is still waiting. Removing that member, or archiving the group, would leave the payment stranded.
- **Options:**
  - a) Also block archive, remove and leave while any pending payment involves that member or group, with the message "Resolve pending payments first". *(Recommended)*
  - b) Check only the balance.
- **Answer:** a (recommended).

### M1-Q8. How the project is created
- **Options:**
  - a) I write the Gradle files by hand (small, and every line can be explained in an interview) and download the Gradle wrapper. This needs network access, which will show an approval prompt. *(Recommended)*
  - b) You create it in Android Studio (New Project → Empty Activity, `com.lokeshreddy.spliteasy`, minSdk 26, Kotlin DSL with a version catalog), and I adapt it.
- **Answer:** a (recommended).

### M1-Q9. Tools not listed in the stack
- **Problem:** The stack lists JUnit, MockK, Turbine and Compose UI tests. M1 needs three more:
  - **Robolectric:** runs the Room DAO tests in `./gradlew test` and in CI without an emulator.
  - **Kover:** measures coverage and fails the build if split and settlement logic drops below 90% (a PRD metric).
  - **ktlint Gradle plugin:** the NFR requires ktlint on every push.
- **Options:**
  - a) Add all three. *(Recommended)*
  - b) Use instrumented DAO tests on an emulator instead of Robolectric (they're slower, not part of `./gradlew test`, and CI would need an emulator).
- **Answer:** a (recommended).

### M1-Q10. GitHub repository
- **Problem:** CI only runs once the code is on GitHub, and a public repo is a PRD metric.
- **Options:**
  - a) Create a public repo `SplitEasy` now with `gh`, signed in to your account, and push after each M1 step. *(Recommended)*
  - b) Keep it local for now; you create the repo later.
- **Answer:** a (recommended).

---

## 3. Technical choices (approve or change)

| Area | Choice | Why |
| --- | --- | --- |
| JDK and targets | Gradle runs on JBR 21 (already installed); Java and Kotlin bytecode target 17 | Supported by current AGP; nothing extra to install |
| SDK levels | minSdk 26; compileSdk and targetSdk 36. API 37 is the latest stable, but its SDK platform isn't installed on this Mac (installing it outside the project was declined), so the move to 37 is a one-line change once it's installed. You chose to stay on API 36, so Compose BOM, core-ktx and lifecycle are pinned to the newest versions that still compile against API 36 (the newest releases need API 37) | PRD compatibility requirement |
| Versions | Latest stable AGP, Kotlin, KSP, Compose BOM, Hilt and Room, checked at setup and pinned in `libs.versions.toml` | Version catalog rule in CLAUDE.md |
| Annotation processing | KSP for Room and Hilt (no kapt) | Faster and current |
| Modules | A single `:app` module, packages `ui/ domain/ data/ sync/ di/` | Q24 and CLAUDE.md |
| Money | Plain `Long` paise everywhere; no `Double` anywhere, including when parsing input | FR-5 and CLAUDE.md |
| Indian number format | Written by hand (lakh grouping) instead of relying on `NumberFormat` locale data | Same output on every device and JVM; easy to test |
| Minus sign | U+2212 "−" and "+" for signed amounts, as in the designs | Matches `05-balances` |
| Dates | `date` stored as a `LocalDate` epoch day (date only); timestamps as epoch milliseconds (UTC) | The designs show dates only; `java.time` is available from API 26 |
| Enums in Room | Stored as their names (strings) through type converters | Reordering an enum won't corrupt data |
| Foreign keys | Only shares → expense (cascade). The other tables use indexed `groupId` columns without foreign keys | Sync can deliver records in any order (for example a member before its group) |
| `createdBy` fields | Store the Firebase Auth uid (a stable local placeholder until M4) | Security rules can compare it to `request.auth.uid` without an extra read |
| Room schema | `exportSchema = true`, JSON files committed under `app/schemas/` | Needed for future migrations and migration tests |
| Domain purity | `domain/` imports no `android.*`, `androidx.*` or Room | CLAUDE.md; checked in review |
| M1 screen | A plain placeholder screen; theme, fonts and dark palette come in M2 | The dark palette needs your approval first (Q19) |
| `HANDOFF.md` | Left in the folder but not committed | It's setup instructions, not part of the app |

---

## 4. Project structure

```
SplitEasy/
├── .github/workflows/ci.yml           # ktlintCheck, lint, test, koverVerify on every push and PR
├── .gitignore                         # build outputs, .gradle, .idea, local.properties, *.jks, *.keystore, google-services.json
├── .editorconfig                      # ktlint settings (allows @Composable function names)
├── CLAUDE.md
├── build.gradle.kts                   # plugins only (apply false)
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml             # every version in one place
│   └── wrapper/
├── docs/                              # PRD, designs, OPEN_QUESTIONS.md, plans/
└── app/
    ├── build.gradle.kts
    ├── schemas/                       # Room schema JSON (committed)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/lokeshreddy/spliteasy/
        │       ├── SplitEasyApp.kt            # @HiltAndroidApp
        │       ├── MainActivity.kt            # placeholder screen in M1
        │       ├── domain/                    # pure Kotlin, no Android imports
        │       │   ├── model/                 # SplitType, Category, SettlementStatus, SettlementMethod, simple data classes
        │       │   ├── money/                 # MoneyFormat (formatInr), MoneyParser, Limits (₹10,00,000 cap)
        │       │   ├── split/                 # SplitCalculator, SplitResult, SplitError
        │       │   ├── balance/               # BalanceCalculator, SettlementSimplifier, PayableAmounts
        │       │   ├── settlement/            # SettlementRules (initial status, confirm, cancel, delete, amount checks)
        │       │   ├── group/                 # GroupRules (permissions, archive/remove/leave checks)
        │       │   └── expense/               # ExpenseRules (amount cap, date window, who can edit)
        │       ├── data/
        │       │   └── local/
        │       │       ├── SplitEasyDatabase.kt
        │       │       ├── Converters.kt
        │       │       ├── entity/            # Group, Member, Expense, ExpenseShare, Settlement, GroupEvent, SyncState
        │       │       └── dao/               # GroupDao, MemberDao, ExpenseDao, SettlementDao, GroupEventDao
        │       ├── di/
        │       │   └── DatabaseModule.kt      # provides the database and DAOs
        │       ├── ui/                        # M2
        │       └── sync/                      # M4
        └── test/java/com/lokeshreddy/spliteasy/
            ├── domain/...                     # JUnit tests, one file per class
            └── data/local/...                 # DAO tests (Robolectric, in-memory Room, Turbine)
```

---

## 5. Step-by-step plan

Each step is one focused commit. Before each commit, `./gradlew ktlintCheck lint test` must pass (plus `koverVerify` from step 3 on).

### Step 1 — Project skeleton
- Write the Gradle files, the version catalog and the wrapper (or adapt the Android Studio project, depending on M1-Q8), with minSdk 26 and the SDK levels from section 3.
- Add the Hilt app class (`SplitEasyApp`) and a placeholder `MainActivity` using Compose and Material 3.
- Run `git init`, add `.gitignore` (including `local.properties`, keystores and `google-services.json`), and leave `HANDOFF.md` untracked.
- **Done when:** `./gradlew assembleDebug` succeeds and the app opens to the placeholder on an emulator.
- **Commit:** `chore: set up Android project with Compose, Hilt and version catalog`

### Step 2 — Quality tooling and CI
- Add the ktlint plugin and `.editorconfig`, Android lint settings (warnings shown, errors fail the build), and Kover with a 90% line-coverage rule on `domain.split`, `domain.balance`, `domain.settlement` and `domain.money`.
- Add a GitHub Actions workflow: on every push and pull request, set up JDK 21 and Gradle with caching, then run `./gradlew ktlintCheck lint test koverVerify`.
- **Done when:** all checks pass locally, and the first CI run is green (if M1-Q10 = a).
- **Commit:** `build: add ktlint, Kover coverage gate and GitHub Actions CI`

### Step 3 — Money formatting and parsing (Q9, section E)
- `formatInr(paise)`: "₹2,400", "₹333.33", "₹0.05", "₹1,00,000". Also a signed version: "+₹2,000" and "−₹1,700".
- `parseInrToPaise(text)`: parses the string directly with no `Double`, accepts at most 2 decimals and commas, and returns an error for anything invalid.
- The ₹10,00,000 cap constant, used by expenses and settlements.
- **Commit:** `feat(domain): add INR formatting and parsing in paise`

### Step 4 — Split calculator (FR-4, FR-5, Q7, Q8)
- `equal(total, participants in member order, payer)`: total ÷ n each; the payer's share gets all leftover paise, or the first participant in member order if the payer isn't in the split.
- `percent(total, basis points per participant, payer)`: each share is `total × bp ÷ 10,000`, rounded down; leftover paise follow the same rule. Basis points must add up to exactly 10,000.
- `exact(total, amounts)`: amounts must add up to the total exactly. The error includes the difference so the UI can show "₹X left to assign".
- Errors: no participants, total ≤ 0, total above the cap, negative share, sums that don't match.
- **Commit:** `feat(domain): add split calculator with deterministic rounding`

### Step 5 — Balances and simplified payments (FR-7, PRD settlement algorithm)
- `BalanceCalculator`: uses the PRD's `net(i)` formula. It counts only expenses that aren't deleted and settlements that are CONFIRMED and not deleted. Pending, rejected and deleted settlements are ignored (Q4, G). Removed members are still counted.
- `SettlementSimplifier`: the greedy method with two priority queues (largest debtor against largest creditor). Ties are broken by member order, so every device shows the same list.
- `PayableAmounts` (Q4, Q5, F4): what you can pay a person is their suggested amount minus your pending payments to them, never below zero. At zero, Pay is disabled.
- **Commit:** `feat(domain): add balance calculation and payment simplification`

### Step 6 — Settlement, group and expense rules (Q2, Q3, Q5, Q6, Q11, Q13, F3–F5, F11, section E)
- `SettlementRules`:
  - Starting status, based on who records it, the payer, the payee, guest status, and UPI or manual.
  - Who may record a payment (payer, payee, or any member when both sides are guests).
  - Confirm or reject: payee only, and only while pending.
  - Cancel: payer only, and only while pending.
  - Delete: payer or payee, once confirmed (guest pairs depend on M1-Q6).
  - Amount check: from 1 paisa up to the payable amount, and no more than the cap.
- `GroupRules`:
  - Any member can rename, add guests and share the invite.
  - Only the creator can archive, unarchive, remove members and revoke invites.
  - Anyone except the creator can leave.
  - The zero-balance checks (plus pending payments, depending on M1-Q7) return the amount still owed, for "Settle ₹X first".
  - An archived group is read-only.
- `ExpenseRules`:
  - Only the creator can edit or delete, and not in an archived group.
  - The amount must be from 1 paisa up to the cap.
  - The date can be at most 30 days in the future.
- **Commit:** `feat(domain): add settlement, group and expense rules`

### Step 7 — Room schema
- Entities match the updated PRD data model, plus whatever M1-Q1 to Q5 add:
  - `GroupEntity`, `MemberEntity`, `ExpenseEntity`
  - `ExpenseShareEntity`, with a composite key (expenseId, memberId)
  - `SettlementEntity`
  - `GroupEventEntity`, append-only
  - `SyncState` (PENDING, SYNCED)
- Add type converters for enums and `LocalDate`, indexes on `groupId` and `syncState`, and `SplitEasyDatabase` v1 with the schema exported.
- Add a Hilt `DatabaseModule`.
- **Commit:** `feat(data): add Room entities and database v1`

### Step 8 — DAOs and DAO tests
- **Reads (Flows):**
  - Active groups, and archived groups.
  - A group's members: current only, or including removed ones.
  - The expense feed (not deleted; by date, newest first).
  - An expense with its shares.
  - A group's settlements and events.
  - The number of PENDING sync records (for the "N changes waiting" banner).
  - The number of settlements waiting for a given user's confirmation, per group (for the F7 badge).
- **Writes:**
  - Upsert an expense and replace its shares in one transaction.
  - Upsert the other entities.
  - Insert-only for events.
  - Soft delete by setting the flag and `updatedAt`.
- **Commit:** `feat(data): add DAOs with Flow queries`

### Step 9 — Wrap up
- Update the PRD data model with the M1-Q1 to Q5 answers.
- Run `./gradlew ktlintCheck lint test koverVerify assembleDebug`, then check CI is green.
- Write a short M1 summary: what was built, the coverage figure, and anything moved to later.
- **Commit:** `docs: update data model after Milestone 1`

---

## 6. Test plan

All tests are JVM unit tests in `app/src/test`.

**Money**
- Formatting: 0 → "₹0", 5 → "₹0.05", 50 → "₹0.50", 2,40,000 → "₹2,400", 2,40,050 → "₹2,400.50", 33,333 → "₹333.33".
- Lakh and crore grouping: "₹1,00,000" and "₹1,00,00,000".
- Signed amounts: "+₹2,000" and "−₹1,700".
- Parsing: "2400", "2,400", "2400.5", "0.01" and " 7 " are accepted. "2400.505", "abc", "-5", "1.2.3" and "" are rejected.

**Split calculator**
- From the design: ₹2,400 split equally 5 ways → ₹480 each.
- ₹100 ÷ 3 with the payer included → payer ₹33.34, others ₹33.33.
- ₹100 ÷ 3 with the payer not in the split → the first participant in member order gets ₹33.34.
- ₹0.01 ÷ 3 → 1 paisa to the payer and 0 to the others.
- ₹1 ÷ 7 → the payer gets 16 paise (14 + 2 leftover).
- One participant.
- Percentages: 33.33 / 33.33 / 33.34 % of ₹100. 99.99% total is rejected (off by 1 basis point). 50 / 50 % of 3 paise → payer 2, other 1. A 0% participant. Negative values and totals over 100% are rejected.
- Exact amounts: exact match; shortfall and excess return the difference; negative amounts are rejected.
- Total of 0, and over the cap, are rejected.
- Property test (1,000 random cases, fixed seed): shares always add up to the total and never go negative.

**Balances and simplification**
- **Design fixture (Coorg trip).** Expenses split equally 5 ways:
  - Cab ₹12,600, paid by Meera
  - Dinner ₹2,400, paid by Priya
  - Fuel ₹3,000, paid by Arjun
  - Homestay ₹6,000, paid by You

  Two earlier confirmed settlements, Priya → Meera ₹2,700 and Ravi → Meera ₹3,100, are made up so the numbers match the design. Plus a pending Ravi → You ₹1,200, which must be ignored.
  - **Expected nets** (as in `05-balances`): Meera +₹2,000, You +₹1,200, Priya +₹300, Ravi −₹1,700, Arjun −₹1,800.
  - **Expected simplified payments:** exactly the design's 4, in order: Arjun → Meera ₹1,800, Ravi → You ₹1,200, Ravi → Priya ₹300, Ravi → Meera ₹200.
- Deleted expenses are excluded. Pending, rejected and deleted settlements are excluded. Removed members are still counted.
- Nets always add up to zero.
- Everyone settled → no payments. Two people → one payment.
- Property test: at most n − 1 payments, and applying them brings every balance to zero.
- Equal balances always give the same order.
- Payable amount: suggested ₹1,200 with ₹1,200 pending → 0 (Pay disabled). Suggested ₹1,500 with ₹1,200 pending → ₹300.

**Rules**
- Settlement starting status for every combination of who records it (payer, payee, third party) and who is a guest (neither, payer, payee, both), for both UPI and manual records.
- Who can confirm, reject, cancel and delete, in each status.
- Amount: 0, 1 paisa, the payable amount, the payable amount + 1, and the cap + 1.
- Each group action for the creator, a member, and in an archived group, with zero and non-zero balances (and pending payments, depending on M1-Q7).
- Expense date: today + 30 days is allowed, + 31 is not. Only the creator can edit. Nobody can edit in an archived group.

**DAOs (Robolectric, in-memory Room, Turbine)**
- Soft-deleted rows are hidden. Archived groups appear only in the archived list.
- Feed order. Replacing shares is atomic.
- The pending-sync count updates when a row changes from PENDING to SYNCED.
- The confirmation badge count is correct per group and per user.
- Events can only be inserted.
- Enum and `LocalDate` values come back unchanged after saving and loading.

---

## 7. Definition of done for M1
- [ ] `./gradlew ktlintCheck lint test koverVerify assembleDebug` is green locally and in CI.
- [ ] Line coverage of `domain.split`, `domain.balance`, `domain.settlement` and `domain.money` is 90% or higher.
- [ ] The Room schema v1 JSON is committed under `app/schemas/`.
- [ ] `domain/` has no Android, AndroidX or Room imports.
- [ ] The app opens to the placeholder screen on an emulator.
- [ ] The PRD data model matches the schema.
- [ ] The work is in small commits with clear messages, one per step above.

## 8. PRD changes already made
I updated `docs/PRD.md` with the review decisions:
- Dropped "phone" from US-2.
- FR-9: the statuses of manually recorded settlements.
- FR-11: how invite links work.
- The security NFR, covering UPI IDs and rules checks.
- The data model: new group, member and expense fields, shares synced inside their expense, and `GroupEventEntity`.
- The sync strategy: the `getAfter()` batch rule, and balance checks running in the app.
- The Balances and Insights screen descriptions, and the settle-up flow on failure.
- A new risk for UPI apps blocking requests.
- Decisions: the free Firebase plan and no push notifications.
- Future scope: in-app account deletion before any Play Store release.

`SettlementEntity` and the M1-Q fields will be updated once section 2 is answered.
