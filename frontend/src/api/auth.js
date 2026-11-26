import { waitForGuestCourseStorage } from '../chatApi.js';
import { blockAdminAction } from './adminReadOnly.js';

async function request(path, options = {}) {
  blockAdminAction(path, options.method);
  let response;
  try {
    response = await fetch(path, { credentials: 'same-origin', cache: 'no-store', ...options });
  } catch {
    throw new Error('서버에 연결할 수 없어요. Spring Boot가 실행 중인지 확인해주세요.');
  }
  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json() : null;
  if (!response.ok) {
    // 서버 로그인이 끊긴 것을 화면 전체(AuthContext)에 알려 메뉴의 로그인 상태를 다시 맞춘다.
    if (response.status === 401 && path !== '/api/auth/me') window.dispatchEvent(new Event('auth:unauthorized'));
    const error = new Error(body?.message || (response.status >= 500
      ? '서버에 연결할 수 없어요. Spring Boot 실행 상태를 확인해주세요.'
      : '요청을 처리하지 못했어요. 새로고침 후 다시 시도해주세요.'));
    error.status = response.status;
    throw error;
  }
  if (response.status !== 204 && body == null) throw new Error('서버 응답을 확인할 수 없어요. API 연결 설정을 확인해주세요.');
  return body;
}

async function postWithCsrf(path, body) {
  // Login/logout rotate CSRF tokens: fetch a fresh token for each mutation.
  const csrf = await request('/api/auth/csrf');
  return request(path, {
    method: 'POST',
    headers: { [csrf.headerName]: csrf.token, 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  });
}

export async function getCurrentMember() {
  try { return await request('/api/auth/me'); }
  catch (error) { if (error.status === 401) return null; throw error; }
}

export async function login(loginId, password) {
  await waitForGuestCourseStorage();
  await postWithCsrf('/api/auth/login', new URLSearchParams({ loginId, password }));
  const member = await getCurrentMember();
  if (!member) throw new Error('로그인 상태를 확인하지 못했어요. 다시 시도해주세요.');
  return member;
}

export async function logout() { await postWithCsrf('/api/auth/logout', new URLSearchParams()); }

async function postJsonWithCsrf(path, data) {
  const csrf = await request('/api/auth/csrf');
  return request(path, {
    method: 'POST',
    headers: { [csrf.headerName]: csrf.token, 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

export async function putJsonWithCsrf(path, data) {
  const csrf = await request('/api/auth/csrf');
  return request(path, {
    method: 'PUT',
    headers: {
      [csrf.headerName]: csrf.token,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });
}


export async function deleteWithCsrf(path) {
  const csrf = await request('/api/auth/csrf');
  return request(path, { method: 'DELETE', headers: { [csrf.headerName]: csrf.token } });
}

export async function getTerms() { return request('/api/auth/terms'); }

export async function checkPhone(phone) { return request(`/api/auth/phone-check?phone=${encodeURIComponent(phone)}`); }
export async function signup(data) { return postJsonWithCsrf('/api/auth/signup', data); }

export async function getOwnerReview() { return request('/api/auth/owner-review'); }
export async function reapplyOwner(data) { return postJsonWithCsrf('/api/auth/owner-review/reapply', data); }

export { request, postJsonWithCsrf };
