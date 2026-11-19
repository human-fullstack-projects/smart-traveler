import { postJsonWithCsrf, request } from '../../../api/auth.js';

export const getBookableStays = () => request('/api/stays?bookable=true');
export const getStay = (id) => request('/api/stays/' + encodeURIComponent(id));
export const getRooms = (id, checkIn, checkOut, guestCount) => {
  const query = new URLSearchParams({ checkIn, checkOut, guestCount });
  return request('/api/stays/' + encodeURIComponent(id) + '/rooms?' + query);
};
export const createReservation = (data) => postJsonWithCsrf('/api/reservations', data);

// 여행자 숙소 찾기 (조건 검색 · 페이지 · 지도) — 서버가 거르고 5/10곳씩만 보냄
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
export const getStayMap = (params) => request('/api/stays/map?' + searchQuery(params));
export const getStayRegions = () => request('/api/stays/regions');
