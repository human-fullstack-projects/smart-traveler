// frontend에서 실행: node src/pages/human_course/tests/verify-state.mjs
import assert from 'node:assert/strict';
import { createServer } from 'vite';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';

const server = await createServer({ server: { middlewareMode: true, hmr: false }, appType: 'custom' });
try {
  const { freshState, transition, creationError, filterCourses } = await server.ssrLoadModule('/src/pages/human_course/data/courseState.js');
  let state = freshState();
  function run(action) { const result = transition(state, action); if (!result.error) state = result.state; return result; }
  assert.equal(filterCourses(state.courses).length, 4);
  assert.equal(filterCourses(state.courses, { region: '부산', category: '맛집', companion: '친구', keyword: '바다' }).length, 1);
  assert.equal(filterCourses(state.courses, { keyword: '  부산 바다  ' }).length, 1, '검색어 앞뒤 공백 무시');
  assert.equal(filterCourses(state.courses, { keyword: '없는 검색어' }).length, 0);
  assert.equal(filterCourses(state.courses, { sort: 'likes' })[0].id, 3);
  const likes = state.courses[0].likes;
  run({ type: 'like', id: 1 }); assert.equal(state.courses[0].likes, likes + 1);
  run({ type: 'like', id: 1 }); assert.equal(state.courses[0].likes, likes);
  assert.ok(run({ type: 'comment', id: 1, text: ' ' }).error);
  run({ type: 'comment', id: 1, text: ' 새 댓글 ', commentId: 'test-comment' });
  assert.equal(state.courses[0].comments.at(-1).text, '새 댓글');
  assert.ok(run({ type: 'delete-comment', id: 1, commentId: 'c1' }).error);
  run({ type: 'delete-comment', id: 1, commentId: 'test-comment' });
  assert.ok(!state.courses[0].comments.some((c) => c.id === 'test-comment'));
  assert.ok(run({ type: 'report', id: 2, reason: '기타' }).error);
  run({ type: 'report', id: 1, reason: '기타', details: '샘플 신고' });
  assert.equal(state.courses[0].visibility, 'PUBLIC', '신고 즉시 숨김 금지');
  assert.ok(run({ type: 'report', id: 1, reason: '기타' }).error);
  run({ type: 'moderate', id: 1 });
  assert.ok(!filterCourses(state.courses).some((c) => c.id === 1));
  assert.ok(run({ type: 'like', id: 1 }).error);
  run({ type: 'moderate', id: 1 }); assert.equal(state.courses[0].reportedByMe, true);
  for (const id of ['R102', 'R103', 'R104', 'R105', 'R106', 'R107']) assert.ok(creationError(state, state.reservations.find((r) => r.id === id)), id);
  assert.equal(run({ type: 'batch' }).count, 1);
  assert.equal(run({ type: 'batch' }).count, 0, '배치 재실행 중복 방지');
  assert.equal(creationError(state, state.reservations.find((r) => r.id === 'R106')), '');
  const fields = { title: '부산 여행', category: '맛집', companion: '친구', summary: '한 줄 소개', body: '여행 후기', photos: ['사진.jpg'] };
  for (const invalid of [
    { title: ' ' }, { title: '가'.repeat(81) }, { summary: ' ' }, { summary: '가'.repeat(151) },
    { body: ' ' }, { body: '가'.repeat(2001) }, { category: '잘못된 유형' }, { companion: '' },
    { photos: Array(6).fill('photo.jpg') },
  ]) assert.ok(run({ type: 'create', reservationId: 'R101', fields: { ...fields, ...invalid } }).error, '잘못된 입력 차단');
  const maxFields = { ...fields, title: '가'.repeat(80), summary: '가'.repeat(150), body: '가'.repeat(2000), photos: Array(5).fill('photo.jpg') };
  assert.ok(!transition(state, { type: 'create', reservationId: 'R101', fields: maxFields }).error, '허용 최대 길이 입력');
  assert.ok(run({ type: 'comment', id: 1, text: '가'.repeat(301) }).error);
  assert.ok(run({ type: 'create', reservationId: 'R101', fields: { ...fields, title: ' ' } }).error);
  const result = run({ type: 'create', reservationId: 'R101', fields });
  assert.ok(result.id);
  const created = structuredClone(state.courses.find((c) => c.id === result.id));
  assert.equal(created.courseVersion, 3);
  assert.equal(created.travelDates, '2026-10-12 ~ 2026-10-14');
  assert.ok(run({ type: 'create', reservationId: 'R101', fields }).error, '예약별 중복 글 방지');
  assert.ok(run({ type: 'edit', id: 1, fields }).error);
  run({ type: 'edit', id: result.id, fields: { ...fields, title: '수정한 제목' } });
  const edited = state.courses.find((c) => c.id === result.id);
  assert.equal(edited.title, '수정한 제목');
  assert.equal(edited.reservationId, created.reservationId);
  assert.deepEqual(edited.plan, created.plan);
  assert.equal(edited.courseVersion, created.courseVersion);
  assert.deepEqual(edited.photos, created.photos);
  run({ type: 'moderate', id: result.id });
  run({ type: 'edit', id: result.id, fields });
  assert.equal(state.courses.find((c) => c.id === result.id).visibility, 'BLINDED');
  for (const [name, props] of [
    ['HumanCourseList', {}], ['HumanCourseDetail', { courseId: 1 }],
    ['HumanCourseCreate', {}], ['HumanCourseEdit', { courseId: result.id }], ['HumanCourseModeration', {}],
  ]) {
    const { default: Page } = await server.ssrLoadModule(`/src/pages/human_course/pages/${name}.jsx`);
    const html = renderToStaticMarkup(React.createElement(Page, { ...state, ...props, isAuthenticated: true, execute() {} }));
    assert.ok(html.includes('<h1>'));
    if (name === 'HumanCourseList') {
      assert.ok(html.includes('aria-pressed="true"'), '동행 필터 선택 상태 제공');
      assert.ok(html.includes('전체 ' + state.courses.length + '건'), '전체/검색 결과 수 제공');
    }
    if (name === 'HumanCourseDetail') {
      assert.ok(html.includes('comment-counter'), '댓글 글자 수 안내 제공');
      assert.ok(html.includes('aria-busy="false"'), '좋아요 처리 상태 제공');
    }
    console.log(`${name}: changed state render OK`);
  }
  const { default: HumanCourseList } = await server.ssrLoadModule('/src/pages/human_course/pages/HumanCourseList.jsx');
  const guestList = renderToStaticMarkup(React.createElement(HumanCourseList, { ...state, isAuthenticated: false, execute() {} }));
  assert.ok(!guestList.includes('#/human-courses/create'), '비로그인 목록에서 작성 버튼 숨김');
  const { default: HumanCourseDetail } = await server.ssrLoadModule('/src/pages/human_course/pages/HumanCourseDetail.jsx');
  const guestDetail = renderToStaticMarkup(React.createElement(HumanCourseDetail, { ...state, courseId: 1, isAuthenticated: false, execute() {} }));
  assert.ok(!guestDetail.includes('게시글 기능'), '비로그인 상세에서 게시글 기능 숨김');
  assert.ok(!guestDetail.includes('comment-form'), '비로그인 상세에서 댓글 작성 숨김');
  assert.ok(!guestDetail.includes('내 댓글 삭제'), '비로그인 상세에서 댓글 삭제 숨김');
  assert.ok(run({ type: 'delete', id: 1 }).error, '타인 글 삭제 방지');
  assert.equal(run({ type: 'delete', id: result.id }).id, result.id);
  assert.ok(!state.courses.some((c) => c.id === result.id));
  assert.ok(run({ type: 'create', reservationId: 'R101', fields }).error, '삭제 후 같은 예약 재작성 방지');
  run({ type: 'reset' }); assert.deepEqual(state, freshState());
  console.log('Filters, likes, comments, reports, create/edit/delete, batch, moderation, reset: OK');
} finally { await server.close(); }
