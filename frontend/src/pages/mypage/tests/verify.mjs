// 실행: frontend에서 node src/pages/mypage/tests/verify.mjs
import assert from 'node:assert/strict';
import { createServer } from 'vite';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';

const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
try {
  const { initialState, reduceState, loadState, canCancel, STORAGE_KEY } = await server.ssrLoadModule('/src/pages/mypage/data/store.js');
  let state = initialState();
  assert.equal(state.bookings.length, 2);
  const upcoming = state.bookings[1];
  assert.ok(canCancel(upcoming));
  const course = { v: 1, name: '샘플', places: ['해변', '전시관', '식당'], note: '첫 생성', time: '2026-11-03' };
  state = reduceState(state, { type: 'course', id: upcoming.id, course });
  assert.equal(state.bookings[1].versions.length, 1);
  state = reduceState(state, { type: 'course', id: upcoming.id, course });
  assert.equal(state.bookings[1].versions.length, 1, '중복 승인 차단');
  state = reduceState(state, { type: 'cancel', id: upcoming.id, status: '환불처리중' });
  assert.equal(state.bookings[1].cancelRequested, true);
  assert.equal(canCancel(state.bookings[1]), false);
  state = reduceState(state, { type: 'course', id: upcoming.id, course: { ...course, v: 2 } });
  assert.equal(state.bookings[1].versions.length, 1, '취소 요청 후 코스 변경 차단');
  const completed = state.bookings[0];
  const post = { title: '수정 제목', body: '수정 본문', photo: '' };
  state = reduceState(state, { type: 'post', id: completed.id, post });
  assert.equal(state.bookings[0].post.title, post.title);
  assert.equal(state.bookings[0].post.version, completed.versions.at(-1).v);
  state = reduceState(state, { type: 'post', id: upcoming.id, post });
  assert.equal(state.bookings[1].post, null, '미숙박 예약 작성 차단');
  state = reduceState(state, { type: 'post', id: completed.id, post: { ...post, title: ' ' } });
  assert.equal(state.bookings[0].post.title, post.title, '공백 제목 차단');
  state = reduceState(state, { type: 'notify', value: false });
  const memory = new Map();
  const storage = { getItem: (key) => memory.get(key), setItem: (key, value) => memory.set(key, value) };
  storage.setItem(STORAGE_KEY, JSON.stringify(state));
  assert.deepEqual(loadState(storage), state, '저장 데이터 복원');
  storage.setItem(STORAGE_KEY, 'broken JSON');
  assert.equal(loadState(storage).bookings.length, 2, '손상 데이터 복구');
  assert.equal(loadState({ getItem() { throw Error('blocked'); } }).notify, true);
  for (const corrupt of [
    { ...completed, date: 'invalid-date' },
    { ...completed, nights: -1 },
    { ...completed, post: { title: {}, body: 'text', photo: '', version: 1 } },
    { ...completed, versions: [null] },
  ]) {
    storage.setItem(STORAGE_KEY, JSON.stringify({ notify: false, bookings: [corrupt] }));
    assert.equal(loadState(storage).bookings.length, 2, '잘못된 저장 구조 복구');
  }
  for (const status of ['취소', '환불실패', '환불처리중']) {
    const fresh = initialState();
    const cancelled = reduceState(fresh, { type: 'cancel', id: fresh.bookings[1].id, status });
    assert.equal(cancelled.bookings[1].status, status);
    assert.equal(cancelled.bookings[1].cancelRequested, true);
  }
  for (const page of ['MyPage', 'Reservations', 'MyCourses', 'MyPosts']) {
    const { default: Page } = await server.ssrLoadModule(`/src/pages/mypage/pages/${page}.jsx`);
    for (const bookings of [initialState().bookings, state.bookings, []]) {
      const html = renderToStaticMarkup(React.createElement(Page, { bookings, notify: state.notify, dispatch() {} }));
      assert.ok(html.includes('<h1>'), `${page} 렌더링`);
    }
    console.log(`${page}: render OK (sample, changed, empty)`);
  }
  console.log('State transitions and storage round-trip: OK');
  const previousWindow = globalThis.window;
  try {
    const { default: App } = await server.ssrLoadModule('/src/App.jsx');
    for (const route of ['/mypage', '/reservations', '/my-courses', '/my-posts', '/', '/conditions']) {
      globalThis.window = { location: { hash: `#${route}` }, localStorage: storage };
      const html = renderToStaticMarkup(React.createElement(App));
      assert.ok(html.includes('<h1>'), `${route}: 앱 전체 렌더링`);
      if (route !== '/' && route !== '/conditions') {
        assert.ok(html.includes(`href="#${route}" aria-current="page"`), `${route}: 현재 메뉴`);
        for (const target of ['mypage', 'reservations', 'my-courses', 'my-posts']) assert.ok(html.includes(`href="#/${target}"`), `${route}: 메뉴 링크 ${target}`);
      }
      console.log(`${route}: App route and navigation OK`);
    }
  } finally {
    if (previousWindow === undefined) delete globalThis.window;
    else globalThis.window = previousWindow;
  }
} finally { await server.close(); }
