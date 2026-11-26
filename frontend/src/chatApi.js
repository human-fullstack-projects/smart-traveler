// 서버 세션의 CSRF 토큰을 함께 보내고 인증 쿠키는 브라우저가 관리한다.
let guestCourseStorage = Promise.resolve();
// 창을 닫을 때는 토큰을 새로 받을 시간이 없어서 마지막으로 받은 토큰을 쓴다.
let lastToken = null;
export function waitForGuestCourseStorage() { return guestCourseStorage; }
async function csrf() {
  // Login and logout rotate session CSRF tokens.
  const response = await fetch('/api/chat/csrf', { credentials: 'same-origin' });
  if (!response.ok) throw new Error('보안 토큰을 불러오지 못했습니다.');
  lastToken = await response.json();
  return lastToken;
}
async function request(path, options = {}) {
  const headers = { ...options.headers };
  if (options.method && options.method !== 'GET') {
    const token = await csrf();
    headers['Content-Type'] = 'application/json'; headers[token.headerName] = token.token;
  }
  const response = await fetch(`/api/chat${path}`, { cache: 'no-store', ...options, headers, credentials: 'same-origin' });
  const text = response.status === 204 || response.headers.get('content-length') === '0' ? '' : await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const error = new Error(body?.message || `서버 요청 실패 (${response.status})`);
    error.status = response.status; error.code = body?.code;
    throw error;
  }
  return body;
}
export const chatApi = {
  courseRegions: () => request('/course-regions'),
  guestCourse: () => request('/guest-course'),
  claimGuestCourse: async () => {
    await guestCourseStorage;
    return request('/guest-course/claim', { method: 'POST' });
  },
  storeGuestCourse: (id, messageId, startDate, courseId) => {
    guestCourseStorage = guestCourseStorage.catch(() => {}).then(() => request(`/sessions/${encodeURIComponent(id)}/recommendations/${encodeURIComponent(messageId)}/guest`, { method: 'POST', body: JSON.stringify({ startDate, courseId }) }));
    return guestCourseStorage;
  },
  saveRecommendedCourse: (id, messageId, startDate, courseId) => request(`/sessions/${encodeURIComponent(id)}/recommendations/${encodeURIComponent(messageId)}/save`,
    { method: 'POST', body: JSON.stringify({ startDate, courseId }) }),
  create: (content, regionId = null, lodgingId = null) => request('/sessions',
    { method: 'POST', body: JSON.stringify({ content, regionId, lodgingId }) }),
  load: (id) => request(`/sessions/${encodeURIComponent(id)}`),
  send: (id, content, regionId = null) => request(`/sessions/${encodeURIComponent(id)}/messages`, { method: 'POST', body: JSON.stringify({ content, regionId }) }),
  // 모아 둔 대화를 DB에 저장한다(대화 종료·다른 페이지 이동).
  flush: (id) => request(`/sessions/${encodeURIComponent(id)}/flush`, { method: 'POST' }),
  // 창·탭을 닫을 때: keepalive로 보내 페이지가 닫혀도 요청이 끝까지 간다. 결과는 기다리지 않는다.
  flushOnExit: (id) => {
    if (!lastToken) return;
    fetch(`/api/chat/sessions/${encodeURIComponent(id)}/flush`, {
      method: 'POST', keepalive: true, credentials: 'same-origin', headers: { [lastToken.headerName]: lastToken.token },
    }).catch(() => {});
  },
  saveConditions: (id, conditions, confirmed) => request(`/sessions/${encodeURIComponent(id)}/conditions`, { method: 'PUT', body: JSON.stringify({ conditionsJson: JSON.stringify(conditions), confirmed }) }),
};
