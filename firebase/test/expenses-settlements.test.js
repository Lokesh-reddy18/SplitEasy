import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';
import { ANANYA, RAVI, STRANGER, expense, seed, seedCoorgTrip, setUpEnvironment, settlement } from './helpers.js';

let env;
const as = (uid) => env.authenticatedContext(uid).firestore();

before(async () => { env = await setUpEnvironment(); });
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

describe('expenses', () => {
  test('members add expenses in their own name', async () => {
    await seedCoorgTrip(env);
    await assertSucceeds(setDoc(doc(as(ANANYA), 'groups/g1/expenses/e1'), expense()));
    await assertFails(setDoc(doc(as(RAVI), 'groups/g1/expenses/e2'), expense({ createdBy: ANANYA })));
    await assertFails(setDoc(doc(as(STRANGER), 'groups/g1/expenses/e3'), expense({ createdBy: STRANGER })));
  });

  test('amounts must be positive and within ₹10,00,000; descriptions are required', async () => {
    await seedCoorgTrip(env);
    await assertFails(setDoc(doc(as(ANANYA), 'groups/g1/expenses/e1'), expense({ amountPaise: 0 })));
    await assertFails(setDoc(doc(as(ANANYA), 'groups/g1/expenses/e1'), expense({ amountPaise: 100000001 })));
    await assertFails(setDoc(doc(as(ANANYA), 'groups/g1/expenses/e1'), expense({ description: '' })));
  });

  test('only the person who added it edits or deletes it (Q11)', async () => {
    await seedCoorgTrip(env);
    await seed(env, 'groups/g1/expenses/e1', expense());
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1/expenses/e1'), { description: 'Mine now', updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(ANANYA), 'groups/g1/expenses/e1'), { isDeleted: true, updatedAt: 2 }));
  });

  test('archived groups are read-only', async () => {
    await seedCoorgTrip(env, { archived: true });
    await assertFails(setDoc(doc(as(ANANYA), 'groups/g1/expenses/e1'), expense()));
  });
});

describe('recording settlements (Q2, Q3, F3)', () => {
  test('the payer records a pending payment; it cannot start confirmed', async () => {
    await seedCoorgTrip(env);
    await assertSucceeds(setDoc(doc(as(RAVI), 'groups/g1/settlements/s1'), settlement()));
    await assertFails(setDoc(doc(as(RAVI), 'groups/g1/settlements/s2'), settlement({ status: 'CONFIRMED' })));
  });

  test('the payee records a confirmed payment', async () => {
    await seedCoorgTrip(env);
    await assertSucceeds(setDoc(doc(as(ANANYA), 'groups/g1/settlements/s1'), settlement({ status: 'CONFIRMED' })));
  });

  test('paying a guest is confirmed straight away', async () => {
    await seedCoorgTrip(env);
    await assertSucceeds(setDoc(doc(as(RAVI), 'groups/g1/settlements/s1'), settlement({ toMemberId: 'm-arjun', status: 'CONFIRMED' })));
  });

  test('anyone records a guest-to-guest payment, but not between other accounts', async () => {
    await seedCoorgTrip(env);
    const guests = settlement({ fromMemberId: 'm-arjun', toMemberId: 'm-meera', status: 'CONFIRMED' });
    await assertSucceeds(setDoc(doc(as(RAVI), 'groups/g1/settlements/s1'), guests));
    const others = settlement({ fromMemberId: 'm-ananya', toMemberId: 'm-arjun', status: 'CONFIRMED' });
    await assertFails(setDoc(doc(as(RAVI), 'groups/g1/settlements/s2'), others));
  });
});

describe('confirming, rejecting, cancelling and undoing (F4, Q6)', () => {
  test('only the payee confirms or rejects', async () => {
    await seedCoorgTrip(env);
    await seed(env, 'groups/g1/settlements/s1', settlement());
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1/settlements/s1'), { status: 'CONFIRMED', updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(ANANYA), 'groups/g1/settlements/s1'), { status: 'REJECTED', updatedAt: 2 }));
  });

  test('only the payer cancels a pending payment', async () => {
    await seedCoorgTrip(env);
    await seed(env, 'groups/g1/settlements/s1', settlement());
    await assertFails(updateDoc(doc(as(ANANYA), 'groups/g1/settlements/s1'), { isDeleted: true, updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(RAVI), 'groups/g1/settlements/s1'), { isDeleted: true, updatedAt: 2 }));
  });

  test('payer or payee undoes a confirmed payment; the amount can never change', async () => {
    await seedCoorgTrip(env);
    await seed(env, 'groups/g1/settlements/s1', settlement({ status: 'CONFIRMED' }));
    await assertFails(updateDoc(doc(as(ANANYA), 'groups/g1/settlements/s1'), { amountPaise: 1, updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(ANANYA), 'groups/g1/settlements/s1'), { isDeleted: true, updatedAt: 2 }));
  });
});
