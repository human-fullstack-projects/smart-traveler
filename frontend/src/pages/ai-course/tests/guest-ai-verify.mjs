import assert from 'node:assert/strict';
import { canStartAi, hasUsedGuestAi, markGuestAiUsed } from '../guestAiTrial.js';

const values = new Map();
const storage = { getItem: key => values.get(key), setItem: (key, value) => values.set(key, value) };
const guest = { member: null, authChecked: true, authCheckFailed: false };
assert.equal(hasUsedGuestAi(storage), false);
assert.equal(canStartAi({ ...guest, used: hasUsedGuestAi(storage) }), true);
markGuestAiUsed(storage);
// 페이지와 관계없이 같은 사용 기록을 조회한다.
for (const page of ['main', 'mypage', 'conditions', 'ai-course']) {
  assert.equal(canStartAi({ ...guest, used: hasUsedGuestAi(storage) }), false, page);
}
assert.equal(canStartAi({ ...guest, member: { memberId: 1 }, used: true }), true);
assert.equal(canStartAi({ ...guest, authChecked: false, used: false }), false);
assert.equal(canStartAi({ ...guest, authCheckFailed: true, used: false }), false);
const blockedStorage = { getItem() { throw new Error('blocked'); }, setItem() { throw new Error('blocked'); } };
assert.equal(hasUsedGuestAi(blockedStorage), false);
assert.doesNotThrow(() => markGuestAiUsed(blockedStorage));
console.log('Guest AI trial checks passed.');
