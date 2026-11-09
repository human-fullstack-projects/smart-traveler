import { request, postJsonWithCsrf } from '../../../api/auth';

export const myPageApi = {
  load(memberId) { return request('/api/mypage', { headers: { 'X-MyPage-Member': String(memberId) } }); },
  importCourses(memberId, courses) {
    return postJsonWithCsrf('/api/mypage/courses/import', { expectedMemberId: memberId, courses });
  },
  course(memberId, id) {
    return request(`/api/mypage/courses/${encodeURIComponent(id)}`, { headers: { 'X-MyPage-Member': String(memberId) } });
  },
};
