import { previewData } from './previewData';

export const DEMO_NOW = '2026-10-26T12:00:00+09:00';
export const USER = '나(샘플 사용자)';
export const statusNames = { COMPLETED: '숙박 완료', CANCELED: '취소된 예약', UPCOMING: '숙박 예정', CONFIRMED: '예약 확정 / 숙박완료 전환 전' };
export const freshState = () => structuredClone(previewData);
export function finalCourse(reservation) {
  if (!reservation?.itineraryFinal) return null;
  return [...(reservation.versions || [{ version: reservation.courseVersion, plan: reservation.plan }])].sort((a, b) => b.version - a.version)[0];
}
export function creationError(state, reservation) {
  if (!reservation) return '예약을 먼저 선택해주세요.';
  if (reservation.status !== 'COMPLETED') return `${statusNames[reservation.status] || reservation.status}은 작성할 수 없습니다.`;
  if (!(Date.parse(reservation.checkoutAt) < Date.parse(DEMO_NOW))) return '체크아웃 시각이 지나야 작성할 수 있습니다.';
  if (!finalCourse(reservation)?.plan?.length) return '최종 확정된 내 코스가 없습니다.';
  if (state.courses.some((course) => course.reservationId === reservation.id)) return '이미 작성한 코스가 있습니다. 기존 글을 수정해주세요.';
  return '';
}
function postFields(input) {
  const fields = { title: String(input.title || '').trim(), summary: String(input.summary || '').trim(), body: String(input.body || '').trim(), category: input.category, companion: input.companion, photos: input.photos || [] };
  if (!fields.title || fields.title.length > 80 || !fields.summary || fields.summary.length > 150 || !fields.body || fields.body.length > 2000) return null;
  if (!['맛집', '문화', '힐링', '액티비티'].includes(fields.category) || !['혼자', '친구', '연인', '가족'].includes(fields.companion)) return null;
  if (!Array.isArray(fields.photos) || fields.photos.length > 5 || fields.photos.some((name) => typeof name !== 'string')) return null;
  return fields;
}
export function filterCourses(courses, filters = {}) {
  const keyword = (filters.keyword || '').trim().toLocaleLowerCase();
  return courses.filter((c) => c.visibility !== 'BLINDED' && (!filters.region || c.region === filters.region) && (!filters.category || c.category === filters.category) && (!filters.companion || c.companion === filters.companion) && (!keyword || `${c.title} ${c.summary} ${c.body}`.toLocaleLowerCase().includes(keyword)))
    .sort(filters.sort === 'likes' ? (a, b) => b.likes - a.likes || b.id - a.id : (a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id);
}

// UI와 분리된 샘플 상태 전환. 실제 서버 권한 검사가 아닙니다.
export function transition(current, action) {
  const state = structuredClone(current);
  const fail = (error) => ({ state: current, error });
  if (action.type === 'reset') return { state: freshState() };
  if (action.type === 'batch') {
    let count = 0;
    state.reservations.forEach((r) => { if (['CONFIRMED', 'UPCOMING', 'CHECKED_IN'].includes(r.status) && Date.parse(r.checkoutAt) < Date.parse(DEMO_NOW)) { r.status = 'COMPLETED'; count++; } });
    return { state, count };
  }
  if (action.type === 'create') {
    const reservation = state.reservations.find((r) => r.id === action.reservationId);
    const error = creationError(state, reservation);
    if (error) return fail(error);
    const fields = postFields(action.fields);
    if (!fields) return fail('필수 항목·글자 수·사진 개수를 확인해주세요.');
    const final = finalCourse(reservation);
    const id = state.nextId++;
    state.courses.push({ ...fields, id, reservationId: reservation.id, author: USER, mine: true, region: reservation.region, plan: structuredClone(final.plan), courseVersion: final.version, visibility: 'PUBLIC', likes: 0, likedByMe: false, reportedByMe: false, comments: [], createdAt: new Date().toISOString().slice(0, 10) });
    return { state, id };
  }
  const course = state.courses.find((c) => String(c.id) === String(action.id));
  if (!course) return fail('게시글을 찾을 수 없습니다.');
  if (action.type === 'moderate') { course.visibility = course.visibility === 'BLINDED' ? 'PUBLIC' : 'BLINDED'; return { state }; }
  if (action.type === 'edit') {
    if (!course.mine) return fail('본인 게시글만 수정할 수 있습니다.');
    const fields = postFields(action.fields);
    if (!fields) return fail('필수 항목·글자 수·사진 개수를 확인해주세요.');
    Object.assign(course, fields);
    return { state, id: course.id };
  }
  if (course.visibility === 'BLINDED') return fail('블라인드 처리된 코스입니다.');
  if (action.type === 'like') { course.likedByMe = !course.likedByMe; course.likes = Math.max(0, course.likes + (course.likedByMe ? 1 : -1)); }
  else if (action.type === 'comment') {
    const text = String(action.text || '').trim();
    if (!text || text.length > 300) return fail('댓글을 1~300자로 입력해주세요.');
    course.comments.push({ id: action.commentId, author: USER, text });
  } else if (action.type === 'delete-comment') {
    const comment = course.comments.find((c) => c.id === action.commentId);
    if (!comment || comment.author !== USER) return fail('본인 댓글만 삭제할 수 있습니다.');
    course.comments = course.comments.filter((c) => c.id !== action.commentId);
  } else if (action.type === 'report') {
    if (course.mine || course.reportedByMe) return fail('본인 글 또는 이미 신고한 글은 신고할 수 없습니다.');
    if (!['광고·홍보', '욕설·비방', '부적절한 내용', '기타'].includes(action.reason)) return fail('신고 사유를 선택해주세요.');
    course.reportedByMe = true; course.reportReason = action.reason; course.reportDetails = String(action.details || '').trim().slice(0, 300);
  } else return fail('지원하지 않는 작업입니다.');
  return { state };
}
