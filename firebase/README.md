# Firestore security rules

`firestore.rules` decides who can read and write SplitEasy's data in Cloud Firestore. The app enforces the same rules first (`GroupRules`, `SettlementRules`, `ExpenseRules` in `:core:domain`); these rules stop anyone who bypasses the app.

## Test (Firestore emulator)

Needs Node 24+ and Java 21.

```sh
cd firebase
npm ci
npm test
```

CI runs the same tests in the `firestore-rules` job.

## Deploy

```sh
cd firebase
npx firebase login          # once, opens a browser
npm run deploy              # firebase deploy --only firestore:rules --project spliteasy-aaef2
```

## Known limitation

Firestore rules can't add up balances, so "everyone must be settled" checks (archive a group, remove a member, leave) run in the app only.
