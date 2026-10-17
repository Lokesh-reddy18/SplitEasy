# Milestone 2 — Core screens and settle-up: plan

**Status:** Draft, waiting for your approval. No code until it's approved and the open questions in section 3 are answered.
**PRD phase:** Core UI and settle-up, weeks 2–3: "Compose screens, balances, UPI".
**Sources:** `docs/PRD.md`, `docs/OPEN_QUESTIONS.md`, `docs/designs/`, `docs/plans/M1-foundations.md` (section 9), `CLAUDE.md`.

**Goal:** by the end of M2 the app works fully offline on one phone. You can create groups, add guests, add, edit and delete expenses, see balances and suggested payments, pay through UPI or record a cash payment, and confirm, reject, cancel or undo settlements. Sign-in and sync between phones come in M4.

---

## 1. Scope

**In M2**
- **Theme:** the design tokens as a Material 3 colour scheme (light and dark), Plus Jakarta Sans bundled (weights 400–800), type scale and shapes from `docs/designs/README.md`, and a theme setting (System, Light, Dark).
- **Shared UI pieces:** cards, amount text (tabular numbers), avatars with initials, category icon tiles, section headers, buttons, the bottom bar, bottom sheets and dialogs.
- **Screens that have designs:**
  - Groups home (02)
  - Group detail (03)
  - Add or edit expense (04)
  - Balances (05)
  - Settle up sheet (06)
  - Profile (09), without the P2 rows "Reminders" and "Export data"
- **Screens to design first** (Q18), each approved by you before it's built:
  - Create group
  - Group settings (rename, members, add guest, a guest's UPI ID, archive, leave)
  - Archived groups
  - Expense detail (with Edit and Delete for its creator)
  - Delete confirmation
  - Edit UPI ID
  - First-time UPI ID prompt
  - Group picker (for "Add expense" outside a group, Q15)
  - Dark palette (Q19)
- **UPI payments:**
  - Build the `upi://pay` link (payee UPI ID, name, amount, note, currency INR).
  - Open the system chooser and read the UPI app's result.
  - SUCCESS → a PENDING settlement with its transaction ID.
  - FAILURE or no response → "Payment didn't go through? Record payment" (Q23).
- **Settlement actions:**
  - Confirm and Not received, by the payee.
  - Cancel, by the payer, while pending.
  - Delete a confirmed settlement (undo), after a confirmation dialog.
  - Settlement rows in the group feed.
  - Group events in the feed: "Arjun set Ravi's UPI ID to …".
- **Data layer:**
  - Repositories write to Room first, mark the change PENDING, and tell a `SyncScheduler`. In M2 that scheduler does nothing; M4 swaps in WorkManager.
  - Mapping between Room entities and domain models.
  - The current user, through a `CurrentUserProvider` (see M2-Q1).
- **ViewModels** with `StateFlow` UI state, plus use cases that combine the M1 domain logic (balances, suggestions, payable amounts and rules).
- **Accessibility and layout:** content descriptions, touch targets of 48 dp or more, font scaling, TalkBack labels for amounts, and a landscape layout with a maximum content width and two columns where it helps.

**Not in M2**
- Sign-in, Firebase and sync: M4.
- Invite links, receipt scan, the widget and Insights charts: M5. The Scan button on Add expense stays hidden until M5.
- PRIVACY.md, the README, the demo GIF and the signed APK: M6.

---

## 2. Firebase: not needed in M2

M2 doesn't use Firebase. Everything runs on Room, which is how the app works offline anyway. The Firebase code (Auth, Firestore, security rules and the sync worker) is M4.

The Firebase console setup is yours to do, and it can happen any time before M4. It takes about 15 minutes, and doing it early means M4 isn't blocked:
1. Create a Firebase project at console.firebase.google.com and stay on the free Spark plan (F7).
2. Add an Android app with package name `com.split.easy`.
3. Add the debug SHA-1 fingerprint, which Google sign-in needs. `./gradlew signingReport` prints it. Add the release key's SHA-1 later, in M6.
4. Turn on **Authentication → Google**.
5. Create a **Firestore** database (production mode, a region near you such as `asia-south1`).
6. Download `google-services.json` into `app/`. It's already git-ignored; don't commit it.

---

## 3. Open questions that block M2

### M2-Q1. Who is "You" before sign-in exists?
- **Problem:** Every `createdBy` and permission check needs a user, but Google sign-in only arrives in M4.
- **Options:**
  - a) M2 uses a fixed local stand-in user (uid `local-user`, name "You"). At the first real sign-in in M4, the M2 test data is wiped, so there's no migration code. Nobody uses the app before M4, so nothing real is lost. *(Recommended)*
  - b) Build a temporary "What's your name?" screen now and migrate the data at sign-in in M4.
- **Answer:**

### M2-Q2. The Insights tab before M5
- **Options:**
  - a) Hide it until M5, so the bottom bar has Groups and Profile only. *(Recommended)*
  - b) Show it with a "Coming soon" screen.
- **Answer:**

### M2-Q3. Is a description required on an expense?
- **Problem:** The feed uses the description as the row title ("Dinner at Raintree").
- **Options:**
  - a) Required, trimmed, at most 60 characters. *(Recommended)*
  - b) Optional; an empty one shows the category name ("Food").
- **Answer:**

### M2-Q4. Order of groups on Groups home
- **Problem:** The design's order (Flat 4B, Coorg trip, Office lunches, Home) isn't alphabetical. M1 sorts by name as a placeholder.
- **Options:**
  - a) Most recent activity first: the latest expense, settlement or group change. *(Recommended)*
  - b) Alphabetical.
- **Answer:**

### M2-Q5. Sync indicators while there's no sync
- **Problem:** Until M4, every change stays PENDING. The offline banner ("2 changes waiting to sync"), the "Not synced" badges and Profile's sync card would show on everything, all the time.
- **Options:**
  - a) Build them now but hide them behind one switch that M4 turns on. *(Recommended)*
  - b) Show them from M2, even though nothing ever syncs yet.
- **Answer:**

### M2-Q6. Paying someone who has no UPI ID
- **Problem:** A real user (not a guest) may not have added a UPI ID yet, and only they can add it (Q1).
- **Options:**
  - a) Disable Pay with the message "Priya hasn't added a UPI ID yet", and keep "Record payment" available. For a guest, offer "Add Ravi's UPI ID" instead, which any member can do (F1). *(Recommended)*
  - b) Hide Pay completely for that person.
- **Answer:**

### M2-Q7. When to show the first-time UPI ID prompt
- **Problem:** Q18 says "the first time the user is asked to pay or receive". Paying doesn't need your own UPI ID; only receiving does.
- **Options:**
  - a) Show a dismissible card on Group detail and Balances when you're owed money and have no UPI ID: "Add your UPI ID so friends can pay you". Once dismissed, it isn't shown again in that group; the same option stays in Profile. *(Recommended)*
  - b) A one-time bottom sheet the first time you're owed money anywhere.
- **Answer:**

### M2-Q8. Add expense defaults
- **Recommended:**
  - Paid by: You.
  - Category: Other (Q12).
  - Date: today.
  - Split: Equally, with every current member ticked.
  - Leaving with unsaved changes asks "Discard this expense?".
- **Answer:**

### M2-Q9. Demo data
- **Options:**
  - a) A "Load demo data" button that exists only in debug builds (in Profile). It creates the four groups and the Coorg trip expenses from the designs, for testing and screenshots. *(Recommended)*
  - b) No demo data.
- **Answer:**

### M2-Q10. Libraries not in the stack yet
- **Problem:** M2 needs three things the stack doesn't list:
  - **DataStore (Preferences)** for the theme choice, the last-used group (Q15), dismissed prompts and the M2 stand-in user.
  - **Kotlin serialization plugin** for type-safe Navigation Compose routes.
  - **Icons:** the designs use simple line icons (inline SVG in the HTML). I'd convert just those into vector drawables instead of adding the large `material-icons-extended` library.
- **Options:**
  - a) Add DataStore and the serialization plugin, and use vector drawables made from the design's icons. *(Recommended)*
  - b) Something different (describe it).
- **Answer:**

---

## 4. Approach

**Architecture (unchanged from CLAUDE.md)**
```
ui/<screen>/   Screen composable + ViewModel (StateFlow<UiState>, one-off events via Channel)
domain/        M1 logic + use cases, e.g. ObserveGroupBalances, SaveExpense, RecordSettlement
data/          Repositories (Room first, mark PENDING, SyncScheduler.requestSync()), mappers
sync/          NoOpSyncScheduler in M2 → WorkManager in M4
di/            Hilt modules
```

**Rules applied on every screen**
- The UI only observes Room Flows. Saves return immediately and the screen updates from the Flow (FR-6).
- Every write sets `updatedAt` and `syncState = PENDING`, using client-generated UUIDs.
- Permissions come from the M1 rules (`GroupRules`, `SettlementRules`, `ExpenseRules`). Actions a user can't take are hidden or disabled, with the reason ("Settle ₹X first").
- Money only appears through `formatInr` and `parseInrToPaise`.

**Testing**
- **ViewModel tests:** JUnit, MockK and Turbine, run against fake repositories.
- **Repository tests:** an in-memory Room database through Robolectric.
- **Compose UI tests**, also through Robolectric so they run in `./gradlew test`, for the key flows:
  - Add expense: validation, and the "₹X of ₹Y assigned" footer
  - Settle up: amount limits
  - Balances: Confirm and Not received
- The 90% coverage gate on split and settlement logic stays.

---

## 5. Step-by-step plan

Each step is one focused commit (or a few small ones). `./gradlew ktlintCheck lint test koverVerify assembleDebug` must pass before each commit.

### Step 1 — Designs for approval (no app code)
- HTML mock-ups in the existing style for the 8 undesigned screens plus a dark-mode palette, rendered to PNG with headless Chrome into `docs/designs/` (numbered 11 onwards). The dark palette goes in `README.md`, with contrast checked at 4.5:1 or more.
- **Stop for your approval** before building any of these screens.
- **Commit:** `docs: add designs for create group, settings, expense detail and dark theme`

### Step 2 — Theme and shared UI pieces
- Colour scheme (light and dark), bundled fonts, typography, shapes, theme setting in DataStore, and the shared composables (section 1).
- **Commit:** `feat(ui): add SplitEasy theme and shared components`

### Step 3 — Repositories, mapping and the current user
- Group, Member, Expense, Settlement and GroupEvent repositories; entity ↔ domain mappers; `CurrentUserProvider` (M2-Q1); `SyncScheduler` interface with a no-op version; use cases that combine balances, suggestions, payable amounts and rules.
- Guest UPI ID changes write the member update and its `GroupEvent` in one Room transaction.
- **Commit:** `feat(data): add repositories that write locally first`

### Step 4 — Navigation and Groups home (02)
- App scaffold, bottom bar and type-safe routes.
- Groups home: totals card, group list with each group's balance, "settled up", the confirmation badge (F7), and links to archived groups.
- Create group: a name plus guests by name; you are added as the creator.
- Group picker for "Add expense" (Q15).
- **Commit:** `feat(ui): add Groups home and create group`

### Step 5 — Group detail (03)
- Balance card with Settle up (Q14 routing) and Balances.
- Feed grouped by date, mixing expenses, settlements and group events. Expenses you're not part of show "not involved"; "Not synced" is behind the M2-Q5 switch.
- **Commit:** `feat(ui): add group detail and activity feed`

### Step 6 — Add, edit and view an expense (04)
- Amount, description, paid by, category, date, and the three split types with live validation and the "₹X of ₹Y assigned" footer.
- Expense detail, with Edit and Delete for the creator only, and a delete confirmation.
- **Commit:** `feat(ui): add expense editor and expense detail`

### Step 7 — Balances (05)
- "Needs your confirmation" (payee), "Waiting for confirmation" (payer, with Cancel), suggested payments (with the Pending chip and paying only the extra amount), and net balances.
- UPI and cash confirmation messages from section G of `OPEN_QUESTIONS.md`.
- **Commit:** `feat(ui): add balances with confirm, reject and cancel`

### Step 8 — Settle up and UPI (06)
- The sheet: payee card with their UPI ID, amount (capped at what's payable), note, "Pay ₹X with UPI app", and "Paid in cash? Record payment".
- Read the UPI result: SUCCESS, FAILURE, cancelled or no response.
- Handle a payee with no UPI ID (M2-Q6) and the first-time prompt (M2-Q7).
- **Commit:** `feat(ui): add settle up sheet with UPI payment`

### Step 9 — Group settings and archived groups
- Rename, members list, add guest, a guest's UPI ID (logged as a group event), remove member, leave, archive and unarchive, all with the "Settle ₹X first" and "Resolve pending payments first" messages.
- **Commit:** `feat(ui): add group settings and archived groups`

### Step 10 — Profile (09) and wrap-up
- Name and avatar, UPI ID (edit updates your member row in every group, F2), theme. Sync card and sign-out are placeholders until M4. No Reminders or Export rows.
- Debug-only demo data (M2-Q9).
- Accessibility pass (TalkBack labels, font scaling at 200%), dark mode check and a landscape check.
- Update the PRD if anything changed, and write the M2 outcome in this doc.
- **Commit:** `feat(ui): add profile` and `docs: Milestone 2 outcome`

---

## 6. Definition of done for M2
- [ ] Every M2 screen matches its design (light and dark), and the 8 new designs were approved before they were built.
- [ ] The full flow works offline on an emulator:
  1. Create a group
  2. Add guests
  3. Add equal, exact and percentage expenses
  4. Check the balances and suggested payments
  5. Pay through UPI, or record a cash payment
  6. Confirm, reject, cancel or undo the settlement
  7. Archive the group
- [ ] Every write lands in Room with `syncState = PENDING` and shows up immediately.
- [ ] ViewModel, repository and key Compose UI tests pass; the domain coverage gate stays at 90% or more.
- [ ] `./gradlew ktlintCheck lint test koverVerify assembleDebug` is green locally and in CI.
- [ ] TalkBack labels on icons and amounts, 48 dp touch targets, and usable at 200% font size and in landscape.
- [ ] The PRD is updated where M2 decisions changed it.
