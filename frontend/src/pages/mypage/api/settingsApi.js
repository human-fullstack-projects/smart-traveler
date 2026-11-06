import { request } from '../../../api/auth';

async function put(path, expectedMemberId, data) {
  const csrf = await request('/api/auth/csrf');
  return request(path, { method: 'PUT', headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: JSON.stringify({ ...data, expectedMemberId }) });
}
export const settingsApi = {
  profile(memberId) { return request('/api/mypage/profile', { headers: { 'X-MyPage-Member': String(memberId) } }); },
  saveProfile(memberId, data) { return put('/api/mypage/profile', memberId, data); },
  preferences(memberId) { return request('/api/mypage/preferences', { headers: { 'X-MyPage-Member': String(memberId) } }); },
  savePreferences(memberId, data) { return put('/api/mypage/preferences', memberId, data); },
};
