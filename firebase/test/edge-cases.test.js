// Cases that check the "group after this batch" logic can't be abused.
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, writeBatch } from 'firebase/firestore';
import { ANANYA, RAVI, STRANGER, expense, group, member, seedCoorgTrip, setUpEnvironment } from './helpers.js';

let env;
const as = (uid) => env.authenticatedContext(uid).firestore();

before(async () => { env = await setUpEnvironment(); });
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

test('a stranger cannot add themselves to a group and write into it in the same batch', async () => {
  await seedCoorgTrip(env);
  const db = as(STRANGER);
  const batch = writeBatch(db);
  batch.update(doc(db, 'groups/g1'), { memberUserIds: [ANANYA, RAVI, STRANGER] });
  batch.set(doc(db, 'groups/g1/expenses/e1'), expense({ createdBy: STRANGER }));
  await assertFails(batch.commit());
});

test('a stranger cannot take over an existing group id by creating it again', async () => {
  await seedCoorgTrip(env);
  await assertFails(setDoc(doc(as(STRANGER), 'groups/g1'), group({ createdBy: STRANGER, memberUserIds: [STRANGER] })));
});

test('a member cannot unarchive a group and add an expense in one batch', async () => {
  await seedCoorgTrip(env, { archived: true });
  const db = as(RAVI);
  const batch = writeBatch(db);
  batch.update(doc(db, 'groups/g1'), { isArchived: false, updatedAt: 2 });
  batch.set(doc(db, 'groups/g1/expenses/e1'), expense({ createdBy: RAVI }));
  await assertFails(batch.commit());
});

test('after leaving, you can no longer read the group', async () => {
  await seedCoorgTrip(env);
  await updateDoc(doc(as(RAVI), 'groups/g1'), { memberUserIds: [ANANYA], updatedAt: 2 });
  await assertFails(getDoc(doc(as(RAVI), 'groups/g1')));
  await assertSucceeds(getDoc(doc(as(ANANYA), 'groups/g1')));
});

test('a stranger cannot create members in a group they are not in', async () => {
  await seedCoorgTrip(env);
  await assertFails(setDoc(doc(as(STRANGER), 'groups/g1/members/m-x'), member('Me', STRANGER)));
});
