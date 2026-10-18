// Shared setup for the rules tests: a test environment, signed-in users and seed data.
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc, writeBatch } from 'firebase/firestore';

export const ANANYA = 'uid-ananya'; // creator of the Coorg trip group
export const RAVI = 'uid-ravi'; // another signed-in member
export const STRANGER = 'uid-stranger'; // not in the group

export async function setUpEnvironment() {
  return initializeTestEnvironment({
    projectId: 'demo-spliteasy',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
}

export const group = (overrides = {}) => ({
  name: 'Coorg trip',
  createdBy: ANANYA,
  memberUserIds: [ANANYA, RAVI],
  isArchived: false,
  lastActivityAt: 1,
  updatedAt: 1,
  isDeleted: false,
  ...overrides,
});

export const member = (displayName, userId, overrides = {}) => ({
  groupId: 'g1',
  displayName,
  userId,
  upiId: null,
  createdAt: 1,
  updatedAt: 1,
  isDeleted: false,
  ...overrides,
});

export const expense = (overrides = {}) => ({
  groupId: 'g1',
  paidBy: 'm-ananya',
  amountPaise: 240000,
  description: 'Dinner at Raintree',
  category: 'FOOD',
  date: '2026-10-10',
  splitType: 'EQUAL',
  createdBy: ANANYA,
  createdAt: 1,
  updatedAt: 1,
  isDeleted: false,
  shares: { 'm-ananya': { paise: 120000, basisPoints: null }, 'm-ravi': { paise: 120000, basisPoints: null } },
  ...overrides,
});

export const settlement = (overrides = {}) => ({
  groupId: 'g1',
  fromMemberId: 'm-ravi',
  toMemberId: 'm-ananya',
  amountPaise: 120000,
  date: '2026-10-10',
  upiTxnId: null,
  status: 'PENDING',
  method: 'UPI',
  updatedAt: 1,
  isDeleted: false,
  ...overrides,
});

/** The Coorg trip: Ananya (creator), Ravi, and two guests, Arjun and Meera. */
export async function seedCoorgTrip(env, { archived = false } = {}) {
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const batch = writeBatch(db);
    batch.set(doc(db, 'groups/g1'), group({ isArchived: archived }));
    batch.set(doc(db, 'groups/g1/members/m-ananya'), member('Ananya', ANANYA));
    batch.set(doc(db, 'groups/g1/members/m-ravi'), member('Ravi', RAVI));
    batch.set(doc(db, 'groups/g1/members/m-arjun'), member('Arjun', null));
    batch.set(doc(db, 'groups/g1/members/m-meera'), member('Meera', null));
    await batch.commit();
  });
}

export async function seed(env, path, data) {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}
