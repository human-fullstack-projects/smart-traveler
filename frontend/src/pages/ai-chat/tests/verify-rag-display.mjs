import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
import { createEmptyConditions } from '../../../travelConditions.js';

const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
try {
  const { default: ChatMessages } = await server.ssrLoadModule('/src/pages/ai-chat/components/ChatMessages.jsx');
  const { default: ChatStart } = await server.ssrLoadModule('/src/pages/ai-chat/components/ChatStart.jsx');
  const { CHAT_INTRO } = await server.ssrLoadModule('/src/pages/ai-chat/chatIntro.js');
  const start = renderToStaticMarkup(React.createElement(ChatStart, { busy: false, onSend() {}, onSelectRegion() {} }));
  assert.ok(start.includes(CHAT_INTRO));
  assert.ok(start.includes('관광지 찾아보기'));
  const content = Array.from({ length: 3 }, (_, course) => [
    `추천 ${course + 1} · 코스 #${course + 1}`, '코스명'.repeat(60),
    ...Array.from({ length: 4 }, (_, day) => [
      `${day + 1}일차 (2026-12-${10 + day})`,
      ...Array.from({ length: 4 }, (_, place) => `${place + 1}. ${'장소'.repeat(100)}`),
    ]).flat(),
  ].join('\n')).join('\n\n');
  assert.ok(content.length > 3000 && content.length < 20000);
  const html = renderToStaticMarkup(React.createElement(ChatMessages, {
    messages: [{ id: 1, role: 'ASSISTANT', content }], busy: false,
  }));
  assert.ok(html.includes(content));
  assert.equal((html.match(/4일차 \(2026-12-13\)/g) || []).length, 3);
  assert.ok(html.includes('코스 #3'));
  const { default: AiChatPage } = await server.ssrLoadModule('/src/pages/ai-chat/AiChatPage.jsx');
  const chat = renderToStaticMarkup(React.createElement(AiChatPage, {
    sessionId: 1, courseSaveStatus: 'ask', conditions: createEmptyConditions(),
    messages: [{ id: 7, role: 'ASSISTANT', content, recommendation: {
      candidateCourseIds: [1, 2, 3], conditions: { startDate: '2026-12-10' },
    } }],
  }));
  assert.equal((chat.match(/type="checkbox"[^>]*\/>코스[123]/g) || []).length, 3);
  assert.ok(chat.indexOf('코스 #3') < chat.indexOf('저장할 코스 선택'));
  assert.match(chat, /disabled="">저장하기/);
  assert.ok(!chat.includes('저장하고 숙소 찾기'), '게스트에게는 예약 바로가기 버튼을 표시하지 않는다');
  console.log(`PASS: all three four-day courses render without truncation (${content.length} characters)`);
  const { default: MyCourses } = await server.ssrLoadModule('/src/pages/mypage/pages/MyCourses.jsx');
  const preview = renderToStaticMarkup(React.createElement(MyCourses, {
    isGuest: true, courses: [], guestRecommendedCourse: { title: '보관한 부산 추천', destination: '부산', startDate: '2026-12-10', durationDays: 2 },
  }));
  assert.ok(preview.includes('보관한 부산 추천') && preview.includes('href="#/login"'));
  assert.ok(!preview.includes('저장된 코스가 없습니다.'));
  console.log('PASS: stored guest recommendation is visible in my-courses with a login link');

  const { buildStayNavHash } = await server.ssrLoadModule('/src/pages/ai-chat/hooks/useAiChat.js');
  const lodgingHash = buildStayNavHash({ sidoId: 10 }, { lodgingId: 5, startDate: '2026-12-10', durationDays: 2, adults: 2, children: 1 }, 12);
  assert.ok(lodgingHash.startsWith('/stays/booking?'));
  assert.ok(lodgingHash.includes('stay=5') && lodgingHash.includes('course=12') && lodgingHash.includes('checkIn=2026-12-10') && lodgingHash.includes('checkOut=2026-12-11') && lodgingHash.includes('children=1'));
  const regionHash = buildStayNavHash({ sidoId: 10, sigunguId: 25, name: '부산광역시 해운대구' }, { lodgingId: null, startDate: '2026-12-10', durationDays: 3, adults: 2, children: 1 }, 34);
  assert.ok(regionHash.startsWith('/stays?') && regionHash.includes('sido=10') && regionHash.includes('sigungu=25'));
  assert.ok(regionHash.includes('course=34') && regionHash.includes('checkIn=2026-12-10') && regionHash.includes('checkOut=2026-12-12') && regionHash.includes('adults=2') && regionHash.includes('children=1'));
  assert.equal(buildStayNavHash(null, { lodgingId: null }, 34), '/stays?course=34');
  assert.equal(buildStayNavHash(null, { lodgingId: null }), null);
  console.log('PASS: saved course navigates to the specific lodging booking page when known, otherwise to the region search page, carrying course id · dates · headcount either way');

  const previousFetch = globalThis.fetch;
  const called = [];
  let releaseStorage;
  globalThis.fetch = async (url, options) => {
    called.push(url);
    const json = value => new Response(JSON.stringify(value), { headers: { 'Content-Type': 'application/json' } });
    if (url.endsWith('/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'test' });
    if (url.endsWith('/guest')) {
      assert.deepEqual(JSON.parse(options.body), { startDate: '2026-12-10', courseId: 2 });
      return new Promise(resolve => { releaseStorage = () => resolve(json({ messageId: 7 })); });
    }
    if (url === '/api/auth/login') return json({ authenticated: true });
    if (url === '/api/auth/me') return json({ memberId: 1, nickname: '회원' });
    throw new Error(`Unexpected request: ${url}`);
  };
  try {
    const { chatApi } = await server.ssrLoadModule('/src/chatApi.js');
    const { login } = await server.ssrLoadModule('/src/api/auth.js');
    const storage = chatApi.storeGuestCourse(1, 7, '2026-12-10', 2);
    const loggedIn = login('test', 'test');
    while (!releaseStorage) await new Promise(resolve => setTimeout(resolve, 0));
    assert.ok(!called.includes('/api/auth/login'));
    releaseStorage();
    await storage;
    assert.equal((await loggedIn).memberId, 1);
    assert.ok(called.indexOf('/api/chat/sessions/1/recommendations/7/guest') < called.indexOf('/api/auth/login'));
    console.log('PASS: login waits for the pending guest storage request');
  } finally { globalThis.fetch = previousFetch; }
} finally { await server.close(); }
