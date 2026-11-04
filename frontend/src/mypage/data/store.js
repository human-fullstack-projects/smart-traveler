import { previewBookings } from './previewBookings';

export const STORAGE_KEY = 'smart-travel-mypage-v1';
function validBooking(b) {
  const text = (value) => typeof value === 'string';
  return b && ['id', 'region', 'date', 'companion', 'transport', 'stay', 'room', 'status'].every((key) => text(b[key]))
    && /^\d{4}-\d{2}-\d{2}$/.test(b.date) && Number.isFinite(Date.parse(`${b.date}T00:00:00Z`))
    && Number.isInteger(b.nights) && b.nights >= 0 && b.nights <= 30
    && Number.isInteger(b.guests) && b.guests > 0 && Number.isFinite(b.price) && b.price >= 0
    && (b.cancelRequested === undefined || typeof b.cancelRequested === 'boolean')
    && Array.isArray(b.versions) && b.versions.every((v, index) => v && v.v === index + 1 && text(v.name) && text(v.note) && text(v.time) && Array.isArray(v.places) && v.places.length >= 3 && v.places.every(text))
    && (b.post == null || (text(b.post.title) && text(b.post.body) && text(b.post.photo) && b.versions.some((v) => v.v === b.post.version)));
}
export function initialState() {
  const future = new Date();
  future.setDate(future.getDate() + 14);
  const booking = { ...previewBookings[0], id: 'sample-upcoming', date: future.toISOString().slice(0, 10), status: '예약확정', post: null, versions: [] };
  return { bookings: structuredClone([...previewBookings, booking]), notify: true };
}

export function loadState(storage) {
  try {
    const value = JSON.parse(storage.getItem(STORAGE_KEY));
    if (typeof value?.notify === 'boolean' && Array.isArray(value.bookings) && value.bookings.every(validBooking) && new Set(value.bookings.map((b) => b.id)).size === value.bookings.length) return value;
  } catch { /* 저장된 데이터가 없거나 손상되면 샘플을 사용합니다. */ }
  return initialState();
}

export function canCancel(booking) {
  return booking.status === '예약확정' && !booking.cancelRequested && Date.now() < new Date(`${booking.date}T15:00:00`).getTime();
}

export function reduceState(state, action) {
  if (action.type === 'notify') return { ...state, notify: action.value };
  return { ...state, bookings: state.bookings.map((b) => {
    if (b.id !== action.id) return b;
    if (action.type === 'cancel' && canCancel(b) && ['환불처리중', '취소', '환불실패'].includes(action.status)) return { ...b, status: action.status, cancelRequested: true };
    const active = b.status !== '취소' && !b.cancelRequested;
    if (action.type === 'course' && active && action.course?.v === (b.versions.at(-1)?.v || 0) + 1) return { ...b, versions: [...b.versions, action.course] };
    if (action.type === 'post' && active && b.status === '숙박완료' && b.versions.length && action.post.title.trim() && action.post.body.trim()) return { ...b, post: { ...action.post, version: b.versions.at(-1).v } };
    return b;
  }) };
}
