// frontend에서 실행: node src/pages/human_course/tests/verify-routes.mjs
import assert from 'node:assert/strict';
import { createServer } from 'vite';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';

const server = await createServer({ server: { middlewareMode: true, hmr: false }, appType: 'custom' });
const previousWindow = globalThis.window;
try {
  const { default: App } = await server.ssrLoadModule('/src/App.jsx');
  const { AuthProvider } = await server.ssrLoadModule('/src/contexts/AuthContext.jsx');
  const renderApp = () => renderToStaticMarkup(React.createElement(AuthProvider, null, React.createElement(App)));
  const cases = [
    ['/human-courses', '다른 여행자의 추천 코스', '#/human-courses/1'],
    ['/human-courses/1', '부산 바다와 맛집을 하루에', '#/human-courses'],
    ['/human-courses/2', '강릉 커피와 바다 산책', '#/human-courses/2/edit'],
    ['/human-courses/create', '여행 코스 공유하기', '#/human-courses/2/edit'],
    ['/human-courses/2/edit', '내 여행 코스 수정', '#/human-courses/2'],
    ['/human-courses/2/edit/', '내 여행 코스 수정', '#/human-courses/2'],
    ['/human-courses/moderation', '신고·블라인드 상태 시연', '#/human-courses/5'],
    ['/human-courses/999', '코스를 찾을 수 없습니다.', '#/human-courses'],
    ['/human-courses/1/edit', '다른 사람의 코스는 수정할 수 없습니다.', '#/human-courses/1'],
    ['/human-courses/5', '블라인드 처리된 코스입니다.', '#/human-courses'],
    ['/human-courses/invalid/path/extra', '페이지를 찾을 수 없습니다.', '#/human-courses'],
  ];
  for (const [route, expected, link] of cases) {
    globalThis.window = { location: { hash: `#${route}` }, localStorage: { getItem() { return null; } } };
    const html = renderApp();
    assert.ok(html.includes(expected), `${route}: content`);
    assert.ok(html.includes(`href="${link}"`), `${route}: navigation`);
    assert.equal((html.match(/<header/g) || []).length, 1, '헤더 중복 없음');
    assert.equal((html.match(/<footer/g) || []).length, 1, '푸터 중복 없음');
    if (route === '/human-courses/5') assert.ok(!html.includes('comment-list'), '블라인드 본문 차단');
    if (route === '/human-courses/1/edit') assert.ok(!html.includes('<form'), '타인 글 수정 폼 차단');
    console.log(`${route}: OK`);
  }
  const { STORAGE_KEY } = await server.ssrLoadModule('/src/pages/human_course/data/courseStorage.js');
  for (const route of ['/human-courses', '/human-courses/create', '/human-courses/moderation', '/human-courses/1', '/human-courses/1/edit']) {
    globalThis.window = { location: { hash: `#${route}` }, localStorage: { getItem: (key) => key === STORAGE_KEY ? JSON.stringify({ version: 1, state: { courses: [], reservations: [], nextId: 1 } }) : null } };
    const html = renderApp();
    assert.ok(html.includes('<h1>'), `${route}: 빈 데이터 렌더링`);
    assert.ok(!html.includes('NaN') && !html.includes('undefined'));
  }
  console.log('Empty lists, reservations, detail/edit not found: OK');
} finally {
  if (previousWindow === undefined) delete globalThis.window;
  else globalThis.window = previousWindow;
  await server.close();
}
