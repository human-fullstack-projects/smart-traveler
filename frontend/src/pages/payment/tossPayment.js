import { postJsonWithCsrf, request } from '../../api/auth.js';

const sdkUrl = 'https://js.tosspayments.com/v2/standard';
let sdkPromise;

export function loadTossSdk() {
  if (window.TossPayments) return Promise.resolve(window.TossPayments);
  if (!sdkPromise) sdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = sdkUrl;
    script.onload = () => window.TossPayments ? resolve(window.TossPayments) : reject(new Error('토스 SDK를 불러오지 못했습니다.'));
    script.onerror = () => reject(new Error('토스 SDK에 연결할 수 없습니다.'));
    document.head.appendChild(script);
  });
  return sdkPromise;
}

export const preparePayment = (reservationId) => postJsonWithCsrf(`/api/payments/reservations/${reservationId}/prepare`, {});
export const confirmPayment = (data) => postJsonWithCsrf('/api/payments/confirm', data);
export const syncPayment = (reservationId) => postJsonWithCsrf(`/api/payments/reservations/${reservationId}/sync`, {});
export const createDemoOrder = (orderName, amount) => postJsonWithCsrf('/api/payments/demo/orders', { orderName, amount });
export const confirmDemoOrder = (data) => postJsonWithCsrf('/api/payments/demo/confirm', data);
export const getDemoOrder = (orderId) => request(`/api/payments/demo/orders/${encodeURIComponent(orderId)}`);
