import { postJsonWithCsrf, request } from '../../../api/auth.js';

export const getBookableStays = () => request('/api/stays?bookable=true');
export const getStay = (id) => request('/api/stays/' + encodeURIComponent(id));
export const getRooms = (id, checkIn, checkOut, guestCount) => {
  const query = new URLSearchParams({ checkIn, checkOut, guestCount });
  return request('/api/stays/' + encodeURIComponent(id) + '/rooms?' + query);
};
export const createReservation = (data) => postJsonWithCsrf('/api/reservations', data);
