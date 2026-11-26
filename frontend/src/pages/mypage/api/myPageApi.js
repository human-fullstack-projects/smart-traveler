import { request, postJsonWithCsrf } from '../../../api/auth';

export const myPageApi = {
  placeDetails(place, region) {
    const query = new URLSearchParams({ name: place.name || '', region: region || '' });
    if (place.placeId) query.set('placeId', String(place.placeId));
    return request(`/api/mypage/places/detail?${query}`);
  },
  placeTourPhotos(place, region) {
    const query = new URLSearchParams({ name: place.name || '', region: region || '', address: place.address || '' });
    if (place.placeId) query.set('placeId', String(place.placeId));
    return request(`/api/mypage/places/tour-photos?${query}`);
  },
  reservationTourPhotos(id) { return request(`/api/mypage/reservations/${encodeURIComponent(id)}/tour-photos`); },
  cancelReservation(id) { return postJsonWithCsrf(`/api/reservations/${encodeURIComponent(id)}/cancel`, {}); },
  load(memberId) { return request('/api/mypage', { headers: { 'X-MyPage-Member': String(memberId) } }); },
  importCourses(memberId, courses) {
    return postJsonWithCsrf('/api/mypage/courses/import', { expectedMemberId: memberId, courses });
  },
  saveHumanItinerary(postId) {
    return postJsonWithCsrf(`/api/mypage/courses/human/${encodeURIComponent(postId)}`, {});
  },
  course(memberId, id) {
    return request(`/api/mypage/courses/${encodeURIComponent(id)}`, { headers: { 'X-MyPage-Member': String(memberId) } });
  },
  booking(memberId, id) {
    return request(`/api/mypage/reservations/${encodeURIComponent(id)}`, { headers: { 'X-MyPage-Member': String(memberId) } });
  },
};
