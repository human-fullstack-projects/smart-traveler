// 서버 세션의 CSRF 토큰을 함께 보내고 인증 쿠키는 브라우저가 관리한다.
let csrfPromise;
async function csrf() {
  if (!csrfPromise) csrfPromise = fetch('/api/chat/csrf', { credentials: 'same-origin' }).then(async (response) => {
    if (!response.ok) throw new Error('보안 토큰을 불러오지 못했습니다.');
    return response.json();
  }).catch((error) => { csrfPromise = null; throw error; });
  return csrfPromise;
}
async function request(path, options = {}) {
  const headers = { ...options.headers };
  if (options.method && options.method !== 'GET') {
    const token = await csrf();
    headers['Content-Type'] = 'application/json'; headers[token.headerName] = token.token;
  }
  const response = await fetch(`/api/chat${path}`, { ...options, headers, credentials: 'same-origin' });
  if (!response.ok) { if (response.status === 403) csrfPromise = null; throw new Error(`서버 요청 실패 (${response.status})`); }
  return response.status === 204 || response.headers.get('content-length') === '0' ? null : response.text().then((text) => text ? JSON.parse(text) : null);
}
export const chatApi = {
  tourPlaces: (province, district, page) => {
    const query = new URLSearchParams({ province, page: String(page) });
    if (district) query.set('district', district);
    return request(`/tour/places?${query}`, { method: 'POST' });
  },
  create: (content) => request('/sessions', { method: 'POST', body: JSON.stringify({ content }) }),
  load: (id) => request(`/sessions/${encodeURIComponent(id)}`),
  send: (id, content) => request(`/sessions/${encodeURIComponent(id)}/messages`, { method: 'POST', body: JSON.stringify({ content }) }),
  saveConditions: (id, conditions, confirmed) => request(`/sessions/${encodeURIComponent(id)}/conditions`, { method: 'PUT', body: JSON.stringify({ conditionsJson: JSON.stringify(conditions), confirmed }) }),
};
