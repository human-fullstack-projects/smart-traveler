// frontend에서 실행: node src/pages/human_course/tests/verify-storage.mjs
import assert from 'node:assert/strict';
import { createServer } from 'vite';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';

const server = await createServer({ server: { middlewareMode: true, hmr: false }, appType: 'custom' });
const previousWindow = globalThis.window;
try {
  const { freshState, transition } = await server.ssrLoadModule('/src/pages/human_course/data/courseState.js');
  const { STORAGE_KEY, validState, loadCourses, saveCourses } = await server.ssrLoadModule('/src/pages/human_course/data/courseStorage.js');
  const memory = new Map([['smart-travel-mypage-v1', 'mypage untouched'], ['human-course-mock-v1', 'original mock untouched']]);
  const storage = { getItem: (key) => memory.get(key) ?? null, setItem: (key, value) => memory.set(key, value) };
  const getStorage = () => storage;
  assert.ok(validState(freshState()), '샘플 구조 유효');
  let state = loadCourses(getStorage).state;
  const fields = { title: '저장할 코스', category: '문화', companion: '친구', summary: '소개', body: '후기', photos: ['여행.jpg'] };
  for (const action of [
    { type: 'like', id: 1 },
    { type: 'comment', id: 1, commentId: 'saved-comment', text: '저장 댓글' },
    { type: 'report', id: 1, reason: '기타', details: '신고 상세' },
    { type: 'moderate', id: 1 },
    { type: 'batch' },
    { type: 'create', reservationId: 'R101', fields },
    { type: 'edit', id: 6, fields: { ...fields, title: '수정한 코스' } },
  ]) {
    const result = transition(state, action);
    assert.ok(!result.error, result.error);
    state = result.state;
    assert.ok(validState(state));
    assert.equal(saveCourses(getStorage, state), '');
    const restored = loadCourses(getStorage);
    assert.equal(restored.notice, '');
    assert.deepEqual(restored.state, state, `${action.type}: 새 초기화에서 상태 복원`);
    state = restored.state;
  }
  assert.equal(state.courses.find((c) => c.id === 6).photos[0], '여행.jpg');
  assert.equal(memory.get('smart-travel-mypage-v1'), 'mypage untouched');
  assert.equal(memory.get('human-course-mock-v1'), 'original mock untouched');
  const { default: App } = await server.ssrLoadModule('/src/App.jsx');
  globalThis.window = { location: { hash: '#/human-courses/6' }, localStorage: storage };
  const html = renderToStaticMarkup(React.createElement(App));
  assert.ok(html.includes('수정한 코스') && html.includes('여행.jpg'), '복원 데이터 상세 렌더링');
  for (const bad of [
    'broken JSON', JSON.stringify({ version: 999, state }),
    JSON.stringify({ version: 1, state: { ...state, courses: [null] } }),
    JSON.stringify({ version: 1, state: { ...state, reservations: [null] } }),
    JSON.stringify({ version: 1, state: { ...state, nextId: 1 } }),
  ]) {
    memory.set(STORAGE_KEY, bad);
    const fallback = loadCourses(getStorage);
    assert.ok(fallback.notice);
    assert.deepEqual(fallback.state, freshState());
    assert.equal(memory.get(STORAGE_KEY), bad, '읽기 실패만으로 원본 덮어쓰기 금지');
  }
  const blocked = () => { throw Error('Storage denied'); };
  assert.ok(loadCourses(blocked).notice);
  assert.ok(saveCourses(blocked, state));
  assert.ok(saveCourses(() => ({ setItem() { throw Error('Quota exceeded'); } }), state));
  globalThis.window = { location: { hash: '#/human-courses' }, get localStorage() { throw Error('Storage denied'); } };
  const warning = renderToStaticMarkup(React.createElement(App));
  assert.ok(warning.includes('role="alert"') && warning.includes('저장 데이터를 읽지 못해'));
  state = transition(state, { type: 'reset' }).state;
  assert.equal(saveCourses(getStorage, state), '');
  assert.deepEqual(loadCourses(getStorage).state, freshState(), '샘플 초기화도 저장');
  console.log('Storage: create/edit, photos, likes, comments, report/blind, batch, reset round-trip OK');
  console.log('Storage: corrupt data, schema mismatch, denied access, quota failure, isolated keys OK');
} finally {
  if (previousWindow === undefined) delete globalThis.window;
  else globalThis.window = previousWindow;
  await server.close();
}
