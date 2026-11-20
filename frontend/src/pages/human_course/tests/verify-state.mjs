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
  assert.equal(filterCourses(state.courses, { keyword: '편한 신발' }).length, 1, '여행 후기 검색');
  assert.equal(filterCourses(state.courses, { keyword: '지역 맛집도 들렀던' }).length, 0, '기존 한 줄 소개는 검색에서 제외');
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
  const fields = { title: '부산 여행', category: '맛집', companion: '친구', body: '여행 후기', photos: ['사진.jpg'] };
  for (const invalid of [
    { title: ' ' }, { title: '가'.repeat(81) },
    { body: ' ' }, { body: '가'.repeat(2001) }, { category: '잘못된 유형' }, { companion: '' },
    { photos: Array(6).fill('photo.jpg') },
  ]) assert.ok(run({ type: 'create', reservationId: 'R101', fields: { ...fields, ...invalid } }).error, '잘못된 입력 차단');
  const maxFields = { ...fields, title: '가'.repeat(80), body: '가'.repeat(2000), photos: Array(5).fill('photo.jpg') };
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
      assert.match(html, /<select id="human-course-companion"[^>]*><option value="" selected="">전체 동행 유형<\/option>/, '동행 필터 기본 선택 상태 제공');
      assert.ok(html.includes('전체 ' + state.courses.length + '건'), '전체/검색 결과 수 제공');
      assert.ok(!html.includes(state.courses[0].summary), '목록은 기존 한 줄 소개 숨김');
    }
    if (name === 'HumanCourseDetail') {
      assert.ok(html.includes('comment-counter'), '댓글 글자 수 안내 제공');
      assert.ok(html.includes('aria-busy="false"'), '좋아요 처리 상태 제공');
      assert.ok(html.includes(state.courses[0].body) && !html.includes(state.courses[0].summary), '상세는 제목과 후기 표시');
    }
    if (name === 'HumanCourseCreate' || name === 'HumanCourseEdit') assert.ok(!html.includes('name="summary"'), '한 줄 소개 입력 제거');
    console.log(`${name}: changed state render OK`);
  }
  const { default: HumanCourseList } = await server.ssrLoadModule('/src/pages/human_course/pages/HumanCourseList.jsx');
  const { default: CourseCard } = await server.ssrLoadModule('/src/pages/human_course/components/CourseCard.jsx');
  const coverOnly = { ...state.courses[0], photoAssets: [], photos: [], coverPhotoUrl: 'https://example.test/tourapi.jpg' };
  const coverHtml = renderToStaticMarkup(React.createElement(CourseCard, { course: coverOnly, detailHref: '#/human-courses/1' }));
  assert.ok(coverHtml.includes('tourapi.jpg') && !coverHtml.includes('등록 사진 0장'), '업로드 사진이 없으면 장소 사진을 표지로 사용');
  const uploadedFirst = { ...coverOnly, photoAssets: [{ name: '올린 사진', dataUrl: '/api/human-courses/photos/first.jpg' }], coverPhotoUrl: '/api/human-courses/photos/first.jpg' };
  const uploadedHtml = renderToStaticMarkup(React.createElement(CourseCard, { course: uploadedFirst }));
  assert.ok(uploadedHtml.includes('/api/human-courses/photos/first.jpg'), '업로드 사진 표지 우선');
  const guestList = renderToStaticMarkup(React.createElement(HumanCourseList, { ...state, isAuthenticated: false, execute() {} }));
  assert.ok(!guestList.includes('#/human-courses/create'), '비로그인 목록에서 작성 버튼 숨김');
  const { default: HumanCourseDetail } = await server.ssrLoadModule('/src/pages/human_course/pages/HumanCourseDetail.jsx');
  const detailWithoutUploads = renderToStaticMarkup(React.createElement(HumanCourseDetail, { courses: [coverOnly], courseId: coverOnly.id, isAuthenticated: false, execute() {} }));
  assert.ok(!detailWithoutUploads.includes('tourapi.jpg'), '상세 등록 사진에는 장소 표지를 섞지 않음');
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
