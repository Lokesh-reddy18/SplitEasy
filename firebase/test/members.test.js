import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc, writeBatch } from 'firebase/firestore';
import { ANANYA, RAVI, STRANGER, member, seedCoorgTrip, setUpEnvironment } from './helpers.js';

let env;
const as = (uid) => env.authenticatedContext(uid).firestore();

before(async () => { env = await setUpEnvironment(); });
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); await seedCoorgTrip(env); });

const upiEvent = (overrides = {}) => ({
  groupId: 'g1',
  actorMemberId: 'm-ravi',
  targetMemberId: 'm-arjun',
  type: 'GUEST_UPI_CHANGED',
  oldValue: null,
  newValue: 'arjun@oksbi',
  createdAt: 2,
  ...overrides,
});

describe('UPI IDs', () => {
  test('you change your own UPI ID', async () => {
    await assertSucceeds(updateDoc(doc(as(RAVI), 'groups/g1/members/m-ravi'), { upiId: 'ravi.k@ybl', updatedAt: 2 }));
  });

  test('nobody changes another account\'s UPI ID (Q1)', async () => {
    await assertFails(updateDoc(doc(as(ANANYA), 'groups/g1/members/m-ravi'), { upiId: 'evil@ybl', updatedAt: 2 }));
  });

  test('any member sets a guest\'s UPI ID when the feed event is written in the same batch (F1)', async () => {
    const db = as(RAVI);
    const batch = writeBatch(db);
    batch.update(doc(db, 'groups/g1/members/m-arjun'), { upiId: 'arjun@oksbi', upiEventId: 'ev1', updatedAt: 2 });
    batch.set(doc(db, 'groups/g1/events/ev1'), upiEvent());
    await assertSucceeds(batch.commit());
  });

  test('a guest\'s UPI ID change without its event is rejected', async () => {
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1/members/m-arjun'), { upiId: 'arjun@oksbi', updatedAt: 2 }));
  });

  test('the event must record the same new UPI ID', async () => {
    const db = as(RAVI);
    const batch = writeBatch(db);
    batch.update(doc(db, 'groups/g1/members/m-arjun'), { upiId: 'arjun@oksbi', upiEventId: 'ev1', updatedAt: 2 });
    batch.set(doc(db, 'groups/g1/events/ev1'), upiEvent({ newValue: 'someone-else@ybl' }));
    await assertFails(batch.commit());
  });

  test('strangers cannot touch a guest', async () => {
    await assertFails(updateDoc(doc(as(STRANGER), 'groups/g1/members/m-arjun'), { displayName: 'Hacked' }));
  });
});

describe('adding and removing people', () => {
  test('any member adds a guest', async () => {
    await assertSucceeds(setDoc(doc(as(RAVI), 'groups/g1/members/m-priya'), member('Priya', null, { createdAt: 5 })));
  });

  test('nobody adds another account as a member (that is the invite flow, M5)', async () => {
    await assertFails(setDoc(doc(as(RAVI), 'groups/g1/members/m-x'), member('Stranger', STRANGER)));
  });

  test('a guest cannot be turned into an account here', async () => {
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1/members/m-arjun'), { userId: RAVI }));
  });

  test('the creator removes someone; others cannot', async () => {
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1/members/m-meera'), { isDeleted: true, updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(ANANYA), 'groups/g1/members/m-meera'), { isDeleted: true, updatedAt: 2 }));
  });

  test('you leave by removing your own row', async () => {
    await assertSucceeds(updateDoc(doc(as(RAVI), 'groups/g1/members/m-ravi'), { isDeleted: true, updatedAt: 2 }));
  });
});

describe('feed events', () => {
  test('events are append-only', async () => {
    const db = as(RAVI);
    const batch = writeBatch(db);
    batch.update(doc(db, 'groups/g1/members/m-arjun'), { upiId: 'arjun@oksbi', upiEventId: 'ev1', updatedAt: 2 });
    batch.set(doc(db, 'groups/g1/events/ev1'), upiEvent());
    await batch.commit();
    await assertFails(updateDoc(doc(db, 'groups/g1/events/ev1'), { newValue: 'changed@ybl' }));
  });

  test('events can only be about guests, written by yourself', async () => {
    await assertFails(setDoc(doc(as(ANANYA), 'groups/g1/events/ev2'), upiEvent({ targetMemberId: 'm-ravi', actorMemberId: 'm-ananya' })));
    await assertFails(setDoc(doc(as(ANANYA), 'groups/g1/events/ev3'), upiEvent({ actorMemberId: 'm-ravi' })));
  });
});
