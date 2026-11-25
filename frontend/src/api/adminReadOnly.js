// 관리자 계정은 여행자 화면을 둘러보기만 한다 (서버 AuthSecurityConfig와 같은 목록).
// 막힌 요청은 서버에 보내지 않고 바로 안내 문구로 끝낸다. 관리자 페이지(/api/admin)는 해당 없음.
export const ADMIN_READ_ONLY_MESSAGE = '관리자는 권한이 없어요.';

let adminViewer = false;
export function setAdminViewer(value) { adminViewer = Boolean(value); }
export function isAdminViewer() { return adminViewer; }

const BLOCKED = [
  ['POST', /^\/api\/reservations$/],
  ['POST', /^\/api\/reservations\/[^/]+\/cancel$/],
  ['POST', /^\/api\/payments\/reservations\/[^/]+\/(prepare|sync)$/],
  ['POST', /^\/api\/payments\/confirm$/],
  ['POST', /^\/api\/human-courses\/reservations\/[^/]+$/],
  ['POST', /^\/api\/human-courses\/[^/]+\/(like|comments|reports)$/],
  ['PUT', /^\/api\/human-courses\/[^/]+$/],
  ['DELETE', /^\/api\/human-courses\/[^/]+(\/comments\/[^/]+)?$/],
  ['POST', /^\/api\/chat\/sessions\/[^/]+\/recommendations\/[^/]+\/save$/],
  ['POST', /^\/api\/chat\/guest-course\/claim$/],
  ['POST', /^\/api\/mypage\/courses\/(human\/[^/]+|import)$/],
  ['PUT', /^\/api\/mypage\/preferences$/],
];

export function blockAdminAction(path, method = 'GET') {
  if (!adminViewer) return;
  const clean = String(path).split('?')[0];
  const m = String(method).toUpperCase();
  if (BLOCKED.some(([verb, pattern]) => verb === m && pattern.test(clean))) {
    const error = new Error(ADMIN_READ_ONLY_MESSAGE);
    error.status = 403;
    error.code = 'ADMIN_READ_ONLY';
    throw error;
  }
}
