// The exact batch shapes the Android uploader sends (core/sync Uploader), checked against the rules.
import { after, before, beforeEach, test } from 'node:test';
import { assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, writeBatch } from 'firebase/firestore';
import { ANANYA, expense, group, member, setUpEnvironment, settlement } from './helpers.js';

let env;
before(async () => { env = await setUpEnvironment(); });
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

test('a new group, its members, an expense and a payment upload in one batch', async () => {
  const db = env.authenticatedContext(ANANYA).firestore();
  const batch = writeBatch(db);
  // Groups first, then members, then expenses and settlements (Uploader order).
  batch.set(doc(db, 'groups/g1'), group({ memberUserIds: [ANANYA] }));
  batch.set(doc(db, 'groups/g1/members/m-ananya'), { ...member('Ananya', ANANYA), upiEventId: null });
  batch.set(doc(db, 'groups/g1/members/m-ravi'), { ...member('Ravi', null), upiEventId: null });
  batch.set(doc(db, 'groups/g1/expenses/e1'), expense());
  batch.set(doc(db, 'groups/g1/settlements/s1'), settlement({ fromMemberId: 'm-ravi', toMemberId: 'm-ananya', status: 'CONFIRMED' }));
  await assertSucceeds(batch.commit());
});

test('a new group with a guest whose UPI ID was set before the first upload', async () => {
  const db = env.authenticatedContext(ANANYA).firestore();
  const batch = writeBatch(db);
  batch.set(doc(db, 'groups/g1'), group({ memberUserIds: [ANANYA] }));
  batch.set(doc(db, 'groups/g1/members/m-ananya'), member('Ananya', ANANYA));
  batch.set(doc(db, 'groups/g1/members/m-arjun'), { ...member('Arjun', null, { upiId: 'arjun@oksbi' }), upiEventId: 'ev1' });
  batch.set(doc(db, 'groups/g1/events/ev1'), {
    groupId: 'g1', actorMemberId: 'm-ananya', targetMemberId: 'm-arjun',
    type: 'GUEST_UPI_CHANGED', oldValue: null, newValue: 'arjun@oksbi', createdAt: 2,
  });
  await assertSucceeds(batch.commit());
});
