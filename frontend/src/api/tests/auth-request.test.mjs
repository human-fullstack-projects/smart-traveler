import assert from 'node:assert/strict';
import test from 'node:test';
import { request } from '../auth.js';

test('예약 수 0을 포함한 정상 JSON 응답을 그대로 반환한다', async (t) => {
  for (const value of [0, false, [], { checkIn: 0, checkOut: 0 }]) {
    t.mock.method(globalThis, 'fetch', async () => Response.json(value));
    assert.deepEqual(await request('/api/owner/reservations/new-count'), value);
    t.mock.restoreAll();
  }
});

test('정상 0건과 비어 있거나 JSON이 아닌 응답을 구분한다', async (t) => {
  for (const response of [Response.json(null), new Response('HTML response')]) {
    t.mock.method(globalThis, 'fetch', async () => response);
    await assert.rejects(request('/api/owner/reservations/new-count'), /서버 응답을 확인할 수 없어요/);
    t.mock.restoreAll();
  }
});

test('본문 없는 204 응답은 허용한다', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 204 }));
  assert.equal(await request('/api/example'), null);
});

test('조회 실패는 0건으로 반환하지 않고 서버 오류를 전달한다', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => Response.json({ message: '조회 실패' }, { status: 503 }));
  await assert.rejects(request('/api/owner/reservations/new-count'), error => error.status === 503 && error.message === '조회 실패');
});

test('통신 단절은 연결 오류로 전달한다', async (t) => {
  t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('Failed to fetch'); });
  await assert.rejects(request('/api/owner/reservations/new-count'), /서버에 연결할 수 없어요/);
});
