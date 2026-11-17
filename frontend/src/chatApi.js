// 서버 세션의 CSRF 토큰을 함께 보내고 인증 쿠키는 브라우저가 관리한다.
let guestCourseStorage = Promise.resolve();
export function waitForGuestCourseStorage() { return guestCourseStorage; }
async function csrf() {
  // Login and logout rotate session CSRF tokens.
  const response = await fetch('/api/chat/csrf', { credentials: 'same-origin' });
  if (!response.ok) throw new Error('보안 토큰을 불러오지 못했습니다.');
  return response.json();
}
async function request(path, options = {}) {
  const headers = { ...options.headers };
  if (options.method && options.method !== 'GET') {
    const token = await csrf();
    headers['Content-Type'] = 'application/json'; headers[token.headerName] = token.token;
  }
  const response = await fetch(`/api/chat${path}`, { cache: 'no-store', ...options, headers, credentials: 'same-origin' });
  if (!response.ok) throw new Error(`서버 요청 실패 (${response.status})`);
  return response.status === 204 || response.headers.get('content-length') === '0' ? null : response.text().then((text) => text ? JSON.parse(text) : null);
}
export const chatApi = {
  courseRegions: () => request('/course-regions'),
  guestCourse: () => request('/guest-course'),
  claimGuestCourse: async () => {
    await guestCourseStorage;
    return request('/guest-course/claim', { method: 'POST' });
  },
  storeGuestCourse: (id, messageId) => {
    guestCourseStorage = guestCourseStorage.catch(() => {}).then(() => request(`/sessions/${encodeURIComponent(id)}/recommendations/${encodeURIComponent(messageId)}/guest`, { method: 'POST' }));
    return guestCourseStorage;
  },
  saveRecommendedCourse: (id, messageId) => request(`/sessions/${encodeURIComponent(id)}/recommendations/${encodeURIComponent(messageId)}/save`, { method: 'POST' }),
  tourPlaces: (province, district, page) => {
    const query = new URLSearchParams({ province, page: String(page) });
    if (district) query.set('district', district);
    return request(`/tour/places?${query}`, { method: 'POST' });
  },
  create: (content, regionId = null) => request('/sessions', { method: 'POST', body: JSON.stringify({ content, regionId }) }),
  load: (id) => request(`/sessions/${encodeURIComponent(id)}`),
  send: (id, content, regionId = null) => request(`/sessions/${encodeURIComponent(id)}/messages`, { method: 'POST', body: JSON.stringify({ content, regionId }) }),
  saveConditions: (id, conditions, confirmed) => request(`/sessions/${encodeURIComponent(id)}/conditions`, { method: 'PUT', body: JSON.stringify({ conditionsJson: JSON.stringify(conditions), confirmed }) }),
};
