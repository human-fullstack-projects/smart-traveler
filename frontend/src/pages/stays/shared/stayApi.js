import { postJsonWithCsrf, request } from '../../../api/auth.js';

export const getBookableStays = () => request('/api/stays?bookable=true');
export const getStay = (id) => request('/api/stays/' + encodeURIComponent(id));
export const getStayTourPhotos = (id) => request('/api/stays/' + encodeURIComponent(id) + '/tour-photos');
export const getRooms = (id, checkIn, checkOut, guestCount) => {
  const query = new URLSearchParams({ checkIn, checkOut, guestCount });
  return request('/api/stays/' + encodeURIComponent(id) + '/rooms?' + query);
};
export const createReservation = (data) => postJsonWithCsrf('/api/reservations', data);

// 여행자 숙소 찾기 — 조건에 맞는 숙소를 한 페이지씩 조회한다.
const searchQuery = (params) => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) { if (value.length) query.set(key, value.join(',')); return; }
    query.set(key, String(value));
  });
  return query.toString();
};
export const searchStays = (params) => request('/api/stays/search?' + searchQuery(params));
export const getStayRegions = () => request('/api/stays/regions');
