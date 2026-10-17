# SplitEasy — Open questions

What I couldn't settle from reading `docs/PRD.md` and every screen in `docs/designs/` (PNGs and HTML).
All items are answered below. These answers are decisions; update `docs/PRD.md` where an answer changes it (Q1, Q10, Q16, Q21).

**Blocks** shows the first milestone that needs the answer:
M1 Foundations · M2 Core UI & settle-up · M4 Auth & sync · M5 Standout features.

The design sample data is consistent: Coorg trip total ₹24,000, your share ₹4,800, net balances sum to zero, and the greedy algorithm gives the 4 suggested payments shown.

---

## A. Settlements and money

### Q1. Who can see a member's UPI ID? — Blocks: M2, M4
- **Source:** PRD Non-functional requirements → Security ("UPI IDs stored only for the owning user"); `06-settle-up-upi` shows the payee's UPI ID (`priya.s@okicici`).
- **Problem:** The payer needs the payee's UPI ID to build the payment link, but the PRD says only the owner keeps it.
- **Options:**
  - a) Only the owner can change it; members of groups they share can read it. *(Recommended)*
  - b) Never shared; the payer types or pastes the payee's UPI ID each time.
- **Answer:** **a.** Only the owner can change their UPI ID; members of groups they share can read it. Update the PRD security line to: "UPI IDs are editable only by their owner and readable only by members of groups they share." Enforce this in Firestore rules. Exception for guests: see F1.

### Q2. Status of manually recorded settlements — Blocks: M1
- **Source:** FR-9, US-8, Settle-up flow step 4 ("payer can add it manually"); `06-settle-up-upi` "Paid in cash? Record payment".
- **Problem:** The PRD only defines the status for a UPI SUCCESS result. It doesn't say what status cash payments recorded by the payer, or debts marked settled by the payee, should get.
- **Options:**
  - a) Recorded by the payer → PENDING until the payee confirms. Recorded by the payee, or the other person is a guest → CONFIRMED straight away. *(Recommended)*
  - b) Every manual record is CONFIRMED straight away.
  - c) Every manual record is PENDING.
- **Answer:** **a.** Recorded by the payer → PENDING until the payee confirms. Recorded by the payee, or the other person is a guest → CONFIRMED straight away.

### Q3. Who confirms a settlement when the payee is a guest? — Blocks: M1
- **Source:** FR-9 ("the payee confirms or rejects it"); PRD Decisions (guest members are tracked by name only).
- **Problem:** Guests have no app, so they can't confirm.
- **Options:**
  - a) Settlements to a guest are confirmed automatically. *(Recommended)*
  - b) Any signed-in member of the group can confirm for the guest.
  - c) Only the group creator can confirm.
- **Answer:** **a.** Settlements to a guest are confirmed automatically. The same applies when the payer is a guest (see Q2).

### Q4. Pending settlements: effect on balances, and paying again — Blocks: M1, M2
- **Source:** `05-balances` (a pending Ravi → You ₹1,200 settlement leaves net balances unchanged and shows a "Pending" chip); Settle-up flow step 5.
- **Problem:** I need to confirm that pending settlements don't change balances, and whether the payer can pay the same debt again while one is pending.
- **Options:**
  - a) Pending doesn't change balances, and Pay is disabled for that amount until it's confirmed or rejected. *(Recommended)*
  - b) Pending doesn't change balances, and Pay is still allowed.
  - c) Pending changes balances straight away and is reversed if rejected.
- **Answer:** **a.** Pending doesn't change balances. Disable Pay for that pair and show "Waiting for <name> to confirm ₹X" until it's confirmed or rejected. If a new debt appears on top of the pending amount, allow paying only the extra.

### Q5. Paying part of a debt, and overpaying — Blocks: M2
- **Source:** `06-settle-up-upi` ("Full balance. You can pay part of it.").
- **Options:**
  - a) Any amount from ₹0.01 up to the full balance. *(Recommended)*
  - b) Any amount; overpaying flips who owes whom.
- **Answer:** **a.** Any amount from ₹0.01 up to the full balance. Block overpaying with an inline error.

### Q6. How is a settlement undone? — Blocks: M1, M2
- **Source:** PRD Risks ("settlement can be undone"); no design for it.
- **Options:**
  - a) Settlements show in the group feed, and the payer or payee can delete (soft delete) a confirmed one. *(Recommended)*
  - b) The only undo is the payee's "Not received" on a pending settlement.
- **Answer:** **a.** Settlements show in the group feed as their own row type. The payer or payee can soft-delete a confirmed one, after a confirmation dialog. Deleting restores the balances and syncs like any other delete.

### Q7. Rounding remainders — Blocks: M1
- **Source:** FR-5, CLAUDE.md ("Rounding remainders go to the payer"), PRD Risks.
- **Problem:** Does the payer's *share* get the extra paise (₹100 split 3 ways → payer 34, others 33)? What if the payer isn't part of the split?
- **Options:**
  - a) The payer's share gets them; if the payer isn't in the split, the first participant in member order does. *(Recommended)*
  - b) The payer's share gets them; if the payer isn't in the split, whoever has the largest share does.
- **Answer:** **a.** The payer's share gets the remainder paise. If the payer isn't in the split, the first participant in member order gets it. The same rule applies to percentage splits. Cover it with unit tests: ₹100 / 3, payer outside the split, and ₹0.01 split 3 ways.

### Q8. Percentage precision — Blocks: M1
- **Source:** FR-4; `04-add-expense` Percent tab.
- **Options:**
  - a) Up to 2 decimals, stored as basis points (33.33% = 3333), and must add up to exactly 100.00%. *(Recommended)*
  - b) Whole percentages only.
- **Answer:** **a.** Up to 2 decimals, stored as basis points (33.33% = 3333), and they must add up to exactly 100.00% (10,000) before Save is enabled.

### Q9. Showing and entering paise — Blocks: M1, M2
- **Source:** Every design shows whole rupees only.
- **Options:**
  - a) Show paise only when they're not zero (₹2,400 and ₹333.33), use Indian digit grouping, and allow typing 2 decimals. *(Recommended)*
  - b) Always show 2 decimals (₹2,400.00).
  - c) Whole rupees only for input; splits still round internally.
- **Answer:** **a.** Show paise only when they're not zero (₹2,400 and ₹333.33), use Indian digit grouping (₹1,00,000), and allow typing up to 2 decimals. Put the formatting in one shared `formatInr(paise: Long)` function with tests.

## B. Data and permissions

### Q10. Fields missing from the data model — Blocks: M1
- **Source:** PRD Data model vs FR-2, FR-10, US-9.
- **Problem:** Groups have no archived flag or syncState (needed to archive and sync). Members have no updatedAt/syncState/isDeleted (needed to remove and sync). Expenses have no createdBy (US-9: "an expense I added"). It's also unclear whether ExpenseShareEntity syncs on its own or as part of its expense.
- **Options:**
  - a) Add only these fields; shares sync as part of their expense. *(Recommended)*
  - b) Stick to the PRD model exactly.
- **Answer:** **a.** Add only these fields: `GroupEntity.isArchived` and `syncState`; `MemberEntity.updatedAt`, `syncState` and `isDeleted`; `ExpenseEntity.createdBy`. Shares sync as part of their expense, in the same Firestore document. Update the PRD data model to match.

### Q11. Who can edit or delete an expense? — Blocks: M1
- **Source:** US-9, FR-3.
- **Options:**
  - a) Only the person who added it. *(Recommended)*
  - b) Any group member.
  - c) The person who added it, or the payer.
- **Answer:** **a.** Only the person who added it. Show other people's expenses as read-only.

### Q12. Category list and icons — Blocks: M1
- **Source:** `03-group-detail`, `08-insights`. Only Food, Travel and Stay appear. "Cab to Coorg" (car icon) and "Fuel" (fuel-pump icon) are both counted as Travel.
- **Options:**
  - a) Fixed list, one icon each: Food, Groceries, Travel, Fuel, Stay, Rent, Utilities, Shopping, Entertainment, Other. *(Recommended)*
  - b) Same list, but the icon comes from keywords in the description (as in the design).
  - c) A different list (give it in the answer).
- **Answer:** **a.** Fixed list with one icon each: Food, Groceries, Travel, Fuel, Stay, Rent, Utilities, Shopping, Entertainment, Other. The user picks the category; no keyword guessing. Default to Other. Insights groups by these categories.

### Q13. Archiving a group or removing a member while money is owed — Blocks: M2
- **Source:** FR-2.
- **Options:**
  - a) Only once settled: all balances at zero to archive; that member's balance at zero to remove. Their old expenses stay. *(Recommended)*
  - b) Allowed with a warning.
- **Answer:** **a.** Only once settled: all balances must be zero to archive a group, and a member's balance must be zero to remove them. Their old expenses stay and show their name. Explain why the button is disabled ("Settle ₹X first").

## C. UI and flows

### Q14. "Settle up" button on Group detail — Blocks: M2
- **Source:** `03-group-detail` shows "Settle up" even though you are owed ₹1,200; `06-settle-up-upi` only covers paying a single person.
- **Options:**
  - a) Open the payment sheet if you owe exactly one person; otherwise open Balances. *(Recommended)*
  - b) Always open Balances.
  - c) Show a sheet listing everyone you owe.
- **Answer:** **a.** Open the payment sheet if you owe exactly one person; otherwise open Balances. If you owe nobody (as in the design), open Balances.

### Q15. "Add expense" outside a group (Groups home, widget "+") — Blocks: M2
- **Source:** `02-groups-home`, `10-widget`; the PRD's Add expense flow starts from Group detail.
- **Options:**
  - a) Ask with a group picker first. *(Recommended)*
  - b) Open in the last-used group, which can be changed.
  - c) Remove the button from home; the widget "+" just opens the app.
- **Answer:** **a.** Ask with a group picker first. Put the last-used group at the top of the list so it's one tap. The widget "+" opens the app at the same picker.

### Q16. Insights content — Blocks: M5
- **Source:** FR-14 ("category and monthly spending charts") vs `08-insights` (category and "Who paid", no monthly chart).
- **Options:**
  - a) Keep the design and add a "By month" bar chart (last 6 months). Period options: a month or All time; group options include "All groups". *(Recommended)*
  - b) Follow the design only (doesn't fully meet FR-14).
- **Answer:** **a.** Keep the design and add a "By month" bar chart for the last 6 months. Period options: a single month or All time. Group options include "All groups". In All groups, show *your share* rather than group totals, so the numbers make sense across groups.

### Q17. How receipt scan works — Blocks: M5
- **Source:** FR-12 (amount and merchant) vs `07-receipt-scan` (also reads the date, live camera with flash).
- **Options:**
  - a) Live camera (CameraX) with flash; fill in amount, merchant (as the description) and date; also allow picking from the gallery. *(Recommended)*
  - b) System camera; fill in amount and merchant only.
- **Answer:** **a.** Live camera (CameraX) with flash; fill in amount, merchant (as the description) and date; also allow picking from the gallery. All fields stay editable. If a field isn't found, leave it blank rather than guessing. Processing stays on the device (ML Kit on-device model).

### Q18. Screens with no design — Blocks: M2, M5
- **Screens:** Create group, Group settings (rename, archive, members), Archived groups list, Edit UPI ID, Invite sheet (README: "follow the Settle up sheet's style"), Delete expense confirmation, First-time UPI ID prompt, Expense detail (or does tapping an expense open Edit?).
- **Options:**
  - a) I design them in the existing style and send screenshots for approval before building. *(Recommended)*
  - b) You'll provide designs.
- **Answer:** **a.** Design them in the existing style and send screenshots for approval before building. Tapping an expense opens an Expense detail screen (with Edit and Delete for its creator), not Edit directly. Show the first-time UPI ID prompt the first time the user is asked to pay or receive, not at sign-in.

### Q19. Dark theme colours — Blocks: M2
- **Source:** Non-functional requirements → Compatibility (light and dark themes); `docs/designs/README.md` only has light colours.
- **Options:**
  - a) I propose a dark palette based on the existing colours for you to approve. *(Recommended)*
  - b) Light only in v1 (doesn't meet the NFR).
- **Answer:** **a.** Propose a dark palette based on the existing colours for approval. Keep the same meaning (violet = owed, amber = owe) and check text contrast is at least 4.5:1.

### Q20. Sign out with unsynced changes — Blocks: M4
- **Source:** `09-profile` (Sign out, "2 changes waiting").
- **Options:**
  - a) Warn if changes are waiting, then clear local data. *(Recommended)*
  - b) Disable sign out until everything is synced.
  - c) Keep local data on the device.
- **Answer:** **a.** If changes are waiting, warn first ("2 changes haven't synced and will be lost") and offer Sync now. Then clear local data on sign out.

## D. Invites, risks and setup

### Q21. How invite links work — Blocks: M5
- **Source:** FR-11, US-2 ("add members by name, phone or invite link"; FR-2 doesn't mention phone). Firebase Dynamic Links shut down in Aug 2025.
- **Options:**
  - a) A web link hosted on Firebase Hosting that opens the app, plus a short code as a fallback. Add members by name only (no phone). *(Recommended)*
  - b) A `splitease://` link plus a code (no hosting, but not clickable in some chat apps).
  - c) Option a) plus a contact picker for name and phone.
- **Answer:** **a.** A web link hosted on Firebase Hosting that opens the app through Android App Links, plus a 6-character code as a fallback. Add members by name only (no phone); drop "phone" from US-2. If the app isn't installed, the web page links to the GitHub release APK. Invite links expire after 7 days and can be revoked from Group settings.

### Q22. Linking a guest to a real account — Blocks: M5
- **Source:** PRD Decisions ("guest members ... can link a real account when they sign up").
- **Options:**
  - a) When joining with an invite link, the new member picks which guest they are (e.g. "I'm Ravi"), or joins as a new member. *(Recommended)*
  - b) The inviter creates the invite link for a specific guest.
- **Answer:** **a.** When joining with an invite link, the new member picks which guest they are (e.g. "I'm Ravi"), or joins as a new member. Linking keeps the guest's member ID, so all history and balances carry over. Only unlinked guests are listed.

### Q23. UPI apps may block payment requests from other apps — Blocks: M2
- **Source:** FR-8, PRD Risks.
- **Problem:** GPay and some other UPI apps often block or fail payment requests from third-party apps to personal UPI IDs, so payments may fail even when our code is right.
- **Options:**
  - a) Build it to the spec; show "Record payment" when payment fails or gets no response; mention the limitation in the README. *(Recommended)*
  - b) Discuss before building UPI.
- **Answer:** **a.** Build it to the spec. When the UPI app returns FAILURE or no response, show "Payment didn't go through? Record payment". Mention the limitation in the README, which is also a good interview talking point.

### Q24. Project setup — Blocks: M1
- **Problem:** There's no Android project or git repo yet.
- **Options:**
  - a) Use `com.lokeshreddy.spliteasy`, a single `:app` module, and set up git with `google-services.json` in `.gitignore`. *(Recommended)*
  - b) A different application ID or setup (give it in the answer).
- **Answer:** **a.** Use `com.lokeshreddy.spliteasy`, a single `:app` module, and set up git with `google-services.json` in `.gitignore`. Also ignore `local.properties` and keystore files. Add a `google-services.json.example` with placeholder values so anyone cloning the repo knows which file to add.

## E. Small points (answered)
- **Terms and Privacy Policy links:** Point both to one `PRIVACY.md` in the GitHub repo for v1 (what data is stored, that receipts are read on the device, how to delete your account). Move it to Firebase Hosting later if needed.
- **Font:** Yes, bundle Plus Jakarta Sans in the app (it's open-licence, OFL). Include only the weights used: 400, 500, 600, 700 and 800.
- **Expense dates:** Allow future dates up to 30 days ahead, since people plan trips. Maximum amount ₹10,00,000 per expense to catch typos.
- **Expenses you're not part of:** Yes, label them "not involved" in the feed, in the muted colour.
- **Landscape:** Adapt the portrait layouts (wider content with a max width, two columns where it helps). No separate designs needed.

## F. Follow-up questions (answered)

### F1. Who sets a guest's UPI ID?
- **Answer:** Any member can set it until the guest links a real account; after that, only the guest can. To stop someone quietly swapping in their own UPI ID, every change to a guest's UPI ID shows in the group feed ("Arjun set Ravi's UPI ID"), and the payment sheet shows the payee's UPI ID before paying (as in the design).

### F2. Where is the UPI ID stored?
- **Answer:** On `MemberEntity.upiId`, as the PRD says. Editing it in Profile updates your member row in every group in one Firestore batch write. No new entity.

### F3. Two guests settle in cash: who records it?
- **Answer:** Any member can record a payment between two guests, and it's CONFIRMED straight away. Otherwise only the payer or payee can record it (Q2 rules).

### F4. Pending settlement edge cases
- **Answer:**
  - The payer can cancel their own pending payment (soft delete; no effect on balances).
  - Pending payments always show in a "Waiting for confirmation" section on Balances, separate from the suggested payments, so they never disappear when suggestions are recalculated.
  - A rejected payment shows in the feed as "Not received", has no effect on balances, and the payer can pay again.

### F5. Group permissions
- **Answer:**
  - Any member can rename the group, add guests and share the invite link.
  - Only the creator can archive, remove members and revoke invites.
  - Anyone except the creator can leave once their balance is zero. The creator can't leave; they archive the group instead.
  - Hide or disable actions the user isn't allowed to do, and enforce the same rules in Firestore security rules.

### F6. Archived groups
- **Answer:** Read-only, moved to an "Archived groups" list (reached from Groups home), and the creator can unarchive. Archived groups are left out of the home totals and the widget.

### F7. Notifications
- **Answer:** No push notifications in v1, and stay on the free Firebase plan (no Cloud Functions). The payee sees pending confirmations inside the app: the "Waiting for confirmation" section on Balances, plus a count badge on the group row on Groups home.

### F8. Account deletion
- **Answer:** No in-app delete in v1. `PRIVACY.md` says to email the developer to delete an account. Note for later: the Play Store requires in-app account deletion, so add it before any Play Store release.

### F9. Insights in "All groups"
- **Answer:** Hide the "Who paid" card in All groups. Never count settlements as spending, in any view.

### F10. Where the guest UPI ID change history is stored
- **Answer:** Add a small append-only `GroupEventEntity`: id, groupId, actorMemberId, targetMemberId, type (`GUEST_UPI_CHANGED`), oldValue, newValue, createdAt. It syncs as its own Firestore collection under the group. Firestore rules allow creating entries only, never editing or deleting them. The group feed shows these events. This is an approved addition to the Q10 field list.

### F11. Balance checks that Firestore rules can't enforce
- **Answer:** Firestore rules enforce all role and ownership checks (creator, payee, expense creator, guest status, UPI ID owner). The "balance must be zero" check for archive, member removal and leave runs in the app, covered by unit tests. List it in the README as a known limitation, fixable later with Cloud Functions.

### F12. Badge for rejected payments
- **Answer:** No. The badge counts only settlements waiting for your confirmation. A rejected payment shows only in the feed as "Not received".

## G. Defaults (confirmed)
- **Theme:** options are System (default), Light and Dark.
- **Pending settlements:** left out of all totals, balances and the widget until confirmed.
- **Invite links:** one active link per group. Creating a new one revokes the old one.
- **Amount cap:** the ₹10,00,000 cap applies to settlements as well as expenses.
- **Confirmation wording:** different for UPI and cash. UPI: "Ravi's UPI app reported this payment as successful. Check that it reached your account, then confirm." Cash: "Ravi says they paid you ₹600 in cash. Confirm once you've received it."

## H. Last checks (answered)

### H1. `GroupEventEntity` needs `syncState`
- **Problem:** F10's field list leaves out `syncState`. Events can be created offline (editing a guest's UPI ID works offline), so the sync worker needs PENDING/SYNCED to know what to upload. No `updatedAt` or `isDeleted` is needed, because events are never edited or deleted.
- **Recommended:** add `syncState`. The guest's member update and its event upload in the same Firestore batch, and the rules check that the event exists in that batch (`existsAfter`), so nobody can change a UPI ID without leaving a record.
- **Answer:** **Recommended, with a note.** Add `syncState`. The guest's member update and its `GroupEventEntity` are written in the same Firestore batch, and the rules use `getAfter()` on the event to require it: it must exist in that batch and its targetMemberId, oldValue and newValue must match the member change. That's what makes the feed trustworthy.

### H2. Section names on Balances
- **Problem:** F7 says the payee sees pending payments in "Waiting for confirmation", but the design (`05-balances`) labels the payee's section "Needs your confirmation".
- **Recommended:** payee sees "Needs your confirmation" (as designed, with Confirm / Not received); payer sees "Waiting for confirmation" (with Cancel).
- **Answer:** **Recommended.** Payee: "Needs your confirmation" (Confirm / Not received), as designed. Payer: "Waiting for confirmation" (Cancel).

### H3. Archived groups in Insights
- **Problem:** The earlier F6 draft included archived groups in Insights for past months; your F6 doesn't mention Insights.
- **Options:** a) Include them, so past spending stays complete. *(Recommended)* b) Exclude them.
- **Answer:** **a.** Include archived groups in Insights so past spending stays complete.

### H4. Removed members on new expenses
- **Problem:** This was in the earlier defaults but isn't in your section G.
- **Recommended:** New expenses list only current members; removed members stay on their old expenses (Q13).
- **Answer:** **Recommended.** New expenses list only current members; removed members stay on their old expenses.

### H5. Feed text for a guest's UPI ID change
- **Problem:** F1's example "Arjun set Ravi's UPI ID" doesn't show the new ID, which is what lets the group spot a swap.
- **Recommended:** "Arjun set Ravi's UPI ID to ravi@okhdfc" the first time, and "Arjun changed Ravi's UPI ID from ravi@okhdfc to arjun@okaxis" after that.
- **Answer:** **Recommended.** First time (no old ID): "Arjun set Ravi's UPI ID to ravi@okhdfc". After that: "Arjun changed Ravi's UPI ID from ravi@okhdfc to arjun@okaxis".
