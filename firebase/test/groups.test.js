import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, getDocs, collection, setDoc, updateDoc, deleteDoc, writeBatch } from 'firebase/firestore';
import { ANANYA, RAVI, STRANGER, group, member, seedCoorgTrip, setUpEnvironment } from './helpers.js';

let env;
const as = (uid) => env.authenticatedContext(uid).firestore();

before(async () => { env = await setUpEnvironment(); });
after(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

describe('reading a group', () => {
  test('members can read it and everything in it', async () => {
    await seedCoorgTrip(env);
    await assertSucceeds(getDoc(doc(as(RAVI), 'groups/g1')));
    await assertSucceeds(getDocs(collection(as(RAVI), 'groups/g1/members')));
  });

  test('strangers and signed-out users cannot', async () => {
    await seedCoorgTrip(env);
    await assertFails(getDoc(doc(as(STRANGER), 'groups/g1')));
    await assertFails(getDocs(collection(as(STRANGER), 'groups/g1/expenses')));
    await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'groups/g1')));
  });
});

describe('creating a group', () => {
  test('the creator writes the group and their own member row together', async () => {
    const db = as(ANANYA);
    const batch = writeBatch(db);
    batch.set(doc(db, 'groups/g2'), group({ memberUserIds: [ANANYA] }));
    batch.set(doc(db, 'groups/g2/members/m1'), member('Ananya', ANANYA, { groupId: 'g2' }));
    batch.set(doc(db, 'groups/g2/members/m2'), member('Priya', null, { groupId: 'g2' }));
    await assertSucceeds(batch.commit());
  });

  test('you cannot create a group in someone else\'s name or without yourself in it', async () => {
    await assertFails(setDoc(doc(as(STRANGER), 'groups/g2'), group({ memberUserIds: [STRANGER] })));
    await assertFails(setDoc(doc(as(ANANYA), 'groups/g2'), group({ memberUserIds: [RAVI] })));
  });
});

describe('changing a group', () => {
  test('any member renames it', async () => {
    await seedCoorgTrip(env);
    await assertSucceeds(updateDoc(doc(as(RAVI), 'groups/g1'), { name: 'Coorg 2026', updatedAt: 2 }));
  });

  test('only the creator archives or unarchives it', async () => {
    await seedCoorgTrip(env);
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1'), { isArchived: true, updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(ANANYA), 'groups/g1'), { isArchived: true, updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(ANANYA), 'groups/g1'), { isArchived: false, updatedAt: 3 }));
  });

  test('an archived group cannot be renamed', async () => {
    await seedCoorgTrip(env, { archived: true });
    await assertFails(updateDoc(doc(as(ANANYA), 'groups/g1'), { name: 'New name', updatedAt: 2 }));
  });

  test('nobody adds other accounts by editing the member list', async () => {
    await seedCoorgTrip(env);
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1'), { memberUserIds: [ANANYA, RAVI, STRANGER] }));
  });

  test('a member can take themselves off the list (leave), but not others', async () => {
    await seedCoorgTrip(env);
    await assertFails(updateDoc(doc(as(RAVI), 'groups/g1'), { memberUserIds: [RAVI] }));
    await assertSucceeds(updateDoc(doc(as(RAVI), 'groups/g1'), { memberUserIds: [ANANYA], updatedAt: 2 }));
  });

  test('the creator cannot change who created it, and nothing is ever hard-deleted', async () => {
    await seedCoorgTrip(env);
    await assertFails(updateDoc(doc(as(ANANYA), 'groups/g1'), { createdBy: RAVI }));
    await assertFails(deleteDoc(doc(as(ANANYA), 'groups/g1')));
  });
});
