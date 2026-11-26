import { request } from '../../../api/auth';

const base = '/api/human-courses';

async function change(path, method, data) {
  const csrf = await request('/api/auth/csrf');
  return request(`${base}${path}`, {
    method,
    headers: { [csrf.headerName]: csrf.token, ...(data === undefined ? {} : { 'Content-Type': 'application/json' }) },
    ...(data === undefined ? {} : { body: JSON.stringify(data) }),
  });
}

export const listCourses = () => request(base);
export const getCourse = (id) => request(`${base}/${encodeURIComponent(id)}`);
export const listEligibleReservations = () => request(`${base}/eligibility`);
export const createCourse = (reservationId, fields) => change(`/reservations/${encodeURIComponent(reservationId)}`, 'POST', fields);
export function editCourse(id, fields, republish, reservationId) {
  const query = new URLSearchParams();
  if (republish) query.set('republish', 'true');
  if (reservationId) query.set('reservationId', reservationId);
  return change(`/${encodeURIComponent(id)}${query.size ? `?${query}` : ''}`, 'PUT', fields);
}
export const deleteCourse = (id) => change(`/${encodeURIComponent(id)}`, 'DELETE');
export const unpublishCourse = (id) => change(`/${encodeURIComponent(id)}/unpublish`, 'POST');
export const likeCourse = (id) => change(`/${encodeURIComponent(id)}/like`, 'POST');
export const commentCourse = (id, text) => change(`/${encodeURIComponent(id)}/comments`, 'POST', { text });
export const deleteCourseComment = (id, commentId) => change(`/${encodeURIComponent(id)}/comments/${encodeURIComponent(commentId)}`, 'DELETE');
export const reportCourse = (id, reason, details) => change(`/${encodeURIComponent(id)}/reports`, 'POST', { reason, details });
