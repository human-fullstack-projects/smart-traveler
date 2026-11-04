import { freshState } from './courseState';

// 마이페이지와 원본 HTML 목업의 저장소를 변경하지 않습니다.
export const STORAGE_KEY = 'smart-travel-human-course-v1';
const VERSION = 1;
const text = (value) => typeof value === 'string';
const positive = (value) => Number.isSafeInteger(value) && value > 0;
const unique = (items, key) => new Set(items.map((item) => item[key])).size === items.length;
const validPlan = (plan) => Array.isArray(plan) && plan.every((day) => day && positive(day.day) && Array.isArray(day.stops) && day.stops.every(text));

export function validState(state) {
  if (!state || !Array.isArray(state.courses) || !Array.isArray(state.reservations) || !positive(state.nextId)) return false;
  const reservationsValid = state.reservations.every((r) => r && ['id', 'name', 'region', 'dates', 'status', 'checkoutAt'].every((key) => text(r[key]))
    && Number.isFinite(Date.parse(r.checkoutAt)) && typeof r.itineraryFinal === 'boolean' && positive(r.courseVersion) && validPlan(r.plan)
    && (r.versions === undefined || (Array.isArray(r.versions) && r.versions.every((v) => v && positive(v.version) && validPlan(v.plan)) && unique(r.versions, 'version'))));
  const coursesValid = state.courses.every((c) => c && positive(c.id) && ['title', 'region', 'category', 'companion', 'summary', 'body', 'author', 'createdAt'].every((key) => text(c[key]))
    && Number.isFinite(Date.parse(c.createdAt)) && typeof c.mine === 'boolean'
    && (c.reservationId === null || text(c.reservationId)) && ['PUBLIC', 'BLINDED'].includes(c.visibility)
    && positive(c.courseVersion) && validPlan(c.plan) && Array.isArray(c.photos) && c.photos.every(text) && c.photos.length <= 5
    && Number.isSafeInteger(c.likes) && c.likes >= 0 && typeof c.likedByMe === 'boolean' && typeof c.reportedByMe === 'boolean'
    && ['reportReason', 'reportDetails', 'moderationReason'].every((key) => c[key] === undefined || text(c[key]))
    && Array.isArray(c.comments) && c.comments.every((comment) => comment && text(comment.id) && text(comment.author) && text(comment.text)) && unique(c.comments, 'id'));
  if (!reservationsValid || !coursesValid || !unique(state.courses, 'id') || !unique(state.reservations, 'id')) return false;
  const linked = state.courses.filter((c) => c.reservationId !== null);
  return unique(linked, 'reservationId') && linked.every((c) => state.reservations.some((r) => r.id === c.reservationId))
    && state.courses.every((c) => c.id < state.nextId);
}

export function loadCourses(getStorage) {
  try {
    const raw = getStorage().getItem(STORAGE_KEY);
    if (raw === null) return { state: freshState(), notice: '' };
    const saved = JSON.parse(raw);
    if (saved?.version !== VERSION || !validState(saved.state)) throw Error('Invalid saved courses');
    return { state: saved.state, notice: '' };
  } catch {
    return { state: freshState(), notice: '저장 데이터를 읽지 못해 샘플 데이터를 표시합니다. 브라우저 저장소가 차단되었거나 데이터가 손상됐을 수 있습니다.' };
  }
}

export function saveCourses(getStorage, state) {
  try {
    getStorage().setItem(STORAGE_KEY, JSON.stringify({ version: VERSION, state }));
    return '';
  } catch {
    return '브라우저에 저장하지 못했습니다. 현재 변경은 화면 이동 중 유지되지만 새로고침하면 사라질 수 있습니다.';
  }
}
