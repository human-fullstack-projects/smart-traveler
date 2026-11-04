import { request, postJsonWithCsrf } from './auth';

function query(params = {}) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.set(k, v); });
  const s = q.toString();
  return s ? `?${s}` : '';
}

export const adminGet = (path, params) => request(`/api/admin${path}${query(params)}`);
export const adminPost = (path, body) => postJsonWithCsrf(`/api/admin${path}`, body);
