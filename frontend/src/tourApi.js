import { chatApi } from './chatApi.js';

// 관광공사 인증키는 서버에만 보관하며 화면에서는 내부 API만 호출한다.
async function read(path, params, signal) {
  const query = new URLSearchParams(Object.entries(params).filter(([, value]) => value !== '' && value != null));
  const response = await fetch(`/api/chat/tour/${path}?${query}`, { signal, credentials: 'same-origin' });
  if (!response.ok) throw new Error('관광정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  return response.json();
}
export const tourApi = {
  regions: (province, signal) => read('regions', { province }, signal),
  places: (province, district, page) => chatApi.tourPlaces(province, district, page),
};
