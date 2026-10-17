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

Not designed yet: invite-to-group sheet (FR-11) — follow the Settle up sheet's style.
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
