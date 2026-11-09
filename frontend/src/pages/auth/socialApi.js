import { postJsonWithCsrf, request } from '../../api/auth';

export function getSocialProviders() { return request('/api/auth/social/providers'); }
export function getPendingSocial() { return request('/api/auth/social/pending'); }
export function socialSignup(data) { return postJsonWithCsrf('/api/auth/social/signup', data); }
