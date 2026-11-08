// 빌드 없이 실행: frontend에서 node src/pages/mypage/tests/verify.mjs
import assert from 'node:assert/strict';
import { createMyPageSession, makeDraft, routeAccess } from '../data/store.js';

const input = { title: '부산 여행', region: '부산', startDate: '2026-12-10', endDate: '2026-12-11', places: '광안리\n전포 거리' };
const deferred = () => { let resolve; let reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; };
const member = id => ({ memberId: id });
function fakeApi() {
  const rows = new Map();
  const calls = [];
  let fail = false;
  return { rows, calls, setFail(value) { fail = value; },
    async importCourses(id, courses) {
      calls.push({ id, courses: structuredClone(courses) });
      if (fail) throw new Error('시험 저장 실패');
      const existing = rows.get(id) || new Map();
      for (const course of courses) if (!existing.has(course.clientId)) existing.set(course.clientId, { ...course, id: `draft-${existing.size + 1}`, state: 'DRAFT' });
      rows.set(id, existing);
      return { importedClientIds: courses.map(c => c.clientId) };
    },
    async load(id) { return { memberId: id, courses: [...(rows.get(id)?.values() || [])], bookings: [], posts: [] }; },
  };
}

const draft = makeDraft(input, 'test-client-id-0001');
assert.equal(draft.days.length, 2);
assert.deepEqual(draft.days[0].places, ['광안리', '전포 거리']);
assert.deepEqual(draft.days[1].places, []);
for (const bad of [{ endDate: '2026-12-09' }, { startDate: '2026-02-30' }, { endDate: '2027-12-11' }, { title: ' ' }, { startDate: 'bad' }]) {
  assert.throws(() => makeDraft({ ...input, ...bad }), '잘못된 초안 차단');
}
const guestAuth = { member: null, authChecked: true, authCheckFailed: false };
assert.equal(routeAccess('/mypage', guestAuth), 'allowed');
assert.equal(routeAccess('/my-courses/guest-id', guestAuth), 'allowed');
for (const route of ['/reservations', '/reservations/7', '/my-posts', '/my-posts/7']) assert.equal(routeAccess(route, guestAuth), 'login');
assert.equal(routeAccess('/my-courses', { ...guestAuth, authChecked: false }), 'checking');
assert.equal(routeAccess('/mypage', { ...guestAuth, authCheckFailed: true }), 'unavailable');

const api = fakeApi();
const store = createMyPageSession(api);
store.setIdentity(null, true, false);
assert.equal(store.getSnapshot().courses.length, 0, '샘플·localStorage 자료를 자동으로 넣지 않음');
await store.addCourse(input);
const guestId = store.getSnapshot().courses[0].clientId;
assert.equal(api.calls.length, 0, '비회원 코스는 서버에 쓰지 않음');
store.setIdentity(null, true, false); // 로그인·가입 등 사이트 내 이동
assert.equal(store.getSnapshot().courses[0].clientId, guestId);
await store.setIdentity(member(7), true, false);
assert.equal(api.calls[0].id, 7);
assert.equal(store.getSnapshot().courses.length, 1);
assert.equal(store.getSnapshot().pendingCount, 0);
await store.refresh();
assert.equal(api.calls.length, 1, '확인된 초안은 재전송하지 않음');
store.setIdentity(null, true, false);
assert.equal(store.getSnapshot().courses.length, 0, '로그아웃 시 회원 자료를 비회원에게 넘기지 않음');
await store.setIdentity(member(8), true, false);
assert.equal(store.getSnapshot().courses.length, 0, '다른 회원에게 코스가 섞이지 않음');
await store.setIdentity(member(7), true, false);
assert.equal(store.getSnapshot().courses.length, 1, '같은 회원은 서버에서 복원');

const failedApi = fakeApi();
const failed = createMyPageSession(failedApi);
failed.setIdentity(null, true, false);
await failed.addCourse(input);
failedApi.setFail(true);
await failed.setIdentity(member(7), true, false);
assert.equal(failed.getSnapshot().status, 'error');
assert.equal(failed.getSnapshot().pendingCount, 1, '실패한 초안을 유지');
const failedId = failedApi.calls[0].courses[0].clientId;
await failed.setIdentity(member(8), true, false);
assert.equal(failedApi.calls.length, 1, '실패한 A회원 초안을 B회원에게 재전송하지 않음');
failedApi.setFail(false);
await failed.setIdentity(member(7), true, false);
assert.equal(failedApi.calls[1].courses[0].clientId, failedId, '재시도 식별자 유지');
assert.equal(failedApi.rows.get(7).size, 1);

// 서버 저장은 성공했지만 응답이 끊긴 경우에도 같은 식별자로 재시도한다.
const lostApi = fakeApi();
const originalImport = lostApi.importCourses;
let loseResponse = true;
lostApi.importCourses = async (...args) => { const result = await originalImport(...args); if (loseResponse) { loseResponse = false; throw Error('응답 유실'); } return result; };
const lost = createMyPageSession(lostApi);
lost.setIdentity(null, true, false);
await lost.addCourse(input);
await lost.setIdentity(member(7), true, false);
assert.equal(lost.getSnapshot().pendingCount, 1);
await lost.refresh();
assert.equal(lostApi.rows.get(7).size, 1, '응답 유실 재시도에서 중복 없음');

const late = deferred();
const lateStore = createMyPageSession({ ...fakeApi(), load(id) { return id === 7 ? late.promise : Promise.resolve({ memberId: id, courses: [], bookings: [], posts: [] }); } });
const firstLoad = lateStore.setIdentity(member(7), true, false);
await lateStore.setIdentity(member(8), true, false);
late.resolve({ memberId: 7, courses: [{ id: 'private-a' }], bookings: [], posts: [] });
await firstLoad;
assert.equal(lateStore.getSnapshot().identity, 8);
assert.equal(lateStore.getSnapshot().courses.length, 0, '이전 회원의 늦은 응답 폐기');

const clearStore = createMyPageSession(fakeApi());
clearStore.setIdentity(null, true, false);
await clearStore.addCourse(input);
clearStore.clear();
assert.equal(clearStore.getSnapshot().courses.length, 0, 'pagehide 처리에서 임시 코스 폐기');
const freshStore = createMyPageSession(fakeApi());
freshStore.setIdentity(null, true, false);
assert.equal(freshStore.getSnapshot().courses.length, 0, '새 페이지 생명주기는 비어 있음');

const wrong = createMyPageSession({ ...fakeApi(), async load() { return { memberId: 99, courses: [{ id: 'private' }], bookings: [], posts: [] }; } });
await wrong.setIdentity(member(7), true, false);
assert.equal(wrong.getSnapshot().status, 'error');
assert.equal(wrong.getSnapshot().courses.length, 0, '응답 회원 불일치 자료 차단');

const lateImport = deferred();
const importApi = fakeApi();
const saveOriginal = importApi.importCourses;
importApi.importCourses = async (...args) => { const result = await saveOriginal(...args); await lateImport.promise; return result; };
const switching = createMyPageSession(importApi);
switching.setIdentity(null, true, false);
await switching.addCourse(input);
const saving = switching.setIdentity(member(7), true, false);
await switching.setIdentity(member(8), true, false);
lateImport.resolve();
await saving;
assert.equal(switching.getSnapshot().identity, 8);
assert.equal(switching.getSnapshot().courses.length, 0, '이전 회원 저장 응답이 새 회원 화면을 덮어쓰지 않음');
assert.equal(importApi.calls.length, 1, '진행 중 초안도 다른 계정에 재전송하지 않음');
await switching.setIdentity(member(7), true, false);
assert.equal(switching.getSnapshot().courses.length, 1);

const partial = createMyPageSession({ ...fakeApi(), async importCourses() { return { importedClientIds: [] }; } });
partial.setIdentity(null, true, false);
await partial.addCourse(input);
await partial.setIdentity(member(7), true, false);
assert.equal(partial.getSnapshot().status, 'error');
assert.equal(partial.getSnapshot().pendingCount, 1, '저장 확인이 누락된 응답은 성공으로 처리하지 않음');

const unauthorized = createMyPageSession({ ...fakeApi(), async load() { throw Object.assign(new Error('세션 만료'), { status: 401 }); } });
await unauthorized.setIdentity(member(7), true, false);
assert.equal(unauthorized.getSnapshot().status, 'login');
unauthorized.setIdentity(null, true, true);
await assert.rejects(() => unauthorized.addCourse(input));
console.log('PASS: 초안 검증, 비회원 접근, 임시 보관, 회원 연결, 실패 재시도, 중복 방지, 계정 분리, 늦은 응답 폐기, 이탈 초기화');
