# SplitEasy designs

Each screen is here twice:
- `png/` — screenshots at 2x (phone frame 390x844 dp; widget 340x180 dp). Look at these first.
- `*.html` — the source markup. Open in a browser. Use it for exact colours, sizes, spacing and copy.
  (PNGs were rendered without the web font, so text uses a fallback; the HTML loads Plus Jakarta Sans.)

| # | Screen | PRD reference | Priority |
| --- | --- | --- | --- |
| 01 | Sign in | FR-1 | P0 |
| 02 | Groups home (with offline sync banner) | FR-2, FR-6, FR-10 | P0 |
| 03 | Group detail (expense feed, "Not synced" badge) | FR-3, FR-6 | P0 |
| 04 | Add expense (equal / exact / percent split) | FR-3, FR-4 | P0 |
| 05 | Balances (pending UPI settlement to confirm, simplified payments, net balances) | FR-7, FR-9 | P0 |
| 06 | Settle up via UPI (bottom sheet) | FR-8, FR-9 | P0 |
| 07 | Receipt scan result | FR-12 | P1 |
| 08 | Insights (category split, who paid) | FR-14 | P1 |
| 09 | Profile and settings | FR-1, FR-10 | P0 |
| 10 | Home-screen widget (Glance) | FR-13 | P1 |

Screens added in Milestone 2 (Q18). They were drawn in the same style and rendered with the web font. **Status: waiting for approval.**

| # | Screen | Decision references | Priority |
| --- | --- | --- | --- |
| 11 | Create group (name, you as creator, guests by name) | FR-2 | P0 |
| 12 | Group settings, creator's view (rename, members, guest UPI ID, archive) | FR-2, F1, F5, Q13 | P0 |
| 13 | Archived groups (read-only, creator can unarchive) | F6 | P0 |
| 14 | Expense detail (Edit and Delete for the person who added it) | Q11, Q18 | P0 |
| 15 | Delete confirmation (also used to undo a settlement) | Q6, Q18 | P0 |
| 16 | Edit your UPI ID (bottom sheet) | Q1, F2 | P0 |
| 17 | Set a guest's UPI ID, with the "everyone will see this" warning | F1, F10, H5 | P0 |
| 18 | First-time UPI ID prompt on Group detail; feed rows for a settlement, a group event and a "not involved" expense | M2-Q7, Q6, F1, E | P0 |
| 19 | Group picker for "Add expense" outside a group | Q15 | P0 |
| 20 | Dark theme, on Groups home; also shows the "1 to confirm" badge and the "Archived groups" link | Q19, F6, F7 | P0 |
| 21 | Settle up when the payee has no UPI ID | M2-Q6 | P0 |
| 22 | Balances for someone who owes: "Waiting for confirmation" with Cancel, and Pay buttons | H2, Q4, F4 | P0 |

Each HTML file starts with a comment explaining the screen's rules and its variants (for example, what non-creators see).
Invite-to-group sheet (FR-11) is designed in Milestone 5, following the Settle up sheet's style.
Profile's "Reminders" and "Export data" rows are P2: hide them in v1.

## Design tokens (map to a Material 3 colour scheme)

| Token | Hex | Use |
| --- | --- | --- |
| primary | #5B3FD6 | Buttons, FAB, links, "you are owed" amounts |
| primaryDark | #4329B0 | Text/icons on primary tint |
| primaryTint | #ECE8FD | Avatars, icon tiles, badges |
| ink | #17142B | Main text, dark summary card |
| inkRaised | #2B2650 | Inner tiles on the dark card |
| muted | #5E5A73 | Secondary text |
| onDarkMuted | #C9C4E6 | Secondary text on dark |
| onDarkOwed | #B9A9FF | "Owed" amount on dark |
| onDarkOwe | #FCD29F | "Owe" amount on dark |
| owe | #B45309 | "You owe" amounts, sign out |
| oweDark | #8A4006 | Text on owe tint, offline banner |
| oweTint | #FEF1E2 | Offline banner, unsynced badge |
| secondary | #0E7490 on #E0F2F5 | Secondary tiles; Travel category |
| food | #D9772F | Food category |
| background | #F6F5FB | Screen background |
| surface | #FFFFFF | Cards, sheets |
| border | #E6E3EF | Card borders |
| divider | #F0EEF5 | Row dividers |

Type: Plus Jakarta Sans (400–800). Screen title 26/800, section title 17/700, body 15/600, caption 13/500, big amount 38–40/800 with tabular numbers.
Shape: cards 16–20 dp corners, buttons 12–16 dp, FAB 18 dp. Touch targets at least 44–48 dp.

## Dark palette (Q19, waiting for approval)

Same meaning as the light theme: violet means you are owed, amber means you owe. Every text pair below was checked
at 4.5:1 contrast or better (WCAG AA). See `20-dark-theme.html`.

| Token | Light | Dark | Contrast in dark |
| --- | --- | --- | --- |
| background | #F6F5FB | #100E1C | |
| surface (cards, sheets, bottom bar) | #FFFFFF | #1B1830 | |
| surfaceRaised (summary card, segmented control) | #17142B | #262140 | |
| inkRaised (tiles inside the summary card) | #2B2650 | #322B55 | |
| ink (main text) | #17142B | #F2F0FA | 15.3:1 on surface |
| muted (secondary text) | #5E5A73 | #A9A4C2 | 7.2:1 on surface |
| primary (filled buttons, FAB) | #5B3FD6 | #6D52E8 | 5.2:1 with white text |
| primary as text ("you are owed", links) | #5B3FD6 | #B9A9FF | 8.4:1 on surface |
| primaryTint / text on it | #ECE8FD / #4329B0 | #2E2858 / #CFC4FF | 8.3:1 |
| owe ("you owe" amounts) | #B45309 | #FCC48A | 11.0:1 on surface |
| oweTint / text on it (offline banner, badges) | #FEF1E2 / #8A4006 | #3A2817 / #FCD29F | 9.9:1 |
| secondary tint / text (Travel) | #E0F2F5 / #0E7490 | #10303A / #7DD3E3 | 8.2:1 |
| food tint / text | #FEF1E2 / #D9772F | #3A2817 / #F0A465 | 6.8:1 |
| border | #E6E3EF | #2E2A47 | |
| divider | #F0EEF5 | #25213A | |

