import { postJsonWithCsrf, request } from '../../api/auth.js';

export function pushSupportMessage() {
  if (!window.isSecureContext) return '브라우저 알림은 HTTPS 또는 localhost에서 테스트해주세요.';
  if (!('Notification' in window) || !('serviceWorker' in navigator) || !('PushManager' in window)) {
    return '이 브라우저에서는 푸시 알림을 지원하지 않아요. Chrome 또는 Edge에서 테스트해주세요.';
  }
  return '';
}

function decodeKey(value) {
  const raw = atob(value.replace(/-/g, '+').replace(/_/g, '/'));
  return Uint8Array.from(raw, (character) => character.charCodeAt(0));
}

function sameKey(current, next) {
  return current && new Uint8Array(current).length === next.length
    && new Uint8Array(current).every((value, index) => value === next[index]);
}

async function activeWorker(registration) {
  if (registration.active) return;
  const worker = registration.installing || registration.waiting;
  if (!worker) throw new Error('알림 기능을 준비하지 못했어요. 새로고침 후 다시 시도해주세요.');
  await new Promise((resolve, reject) => {
    const finish = (error) => {
      clearTimeout(timeout);
      worker.removeEventListener('statechange', changed);
      error ? reject(error) : resolve();
    };
    const changed = () => {
      if (worker.state === 'activated') finish();
      else if (worker.state === 'redundant') finish(new Error('알림 기능을 준비하지 못했어요. 다시 시도해주세요.'));
    };
    const timeout = setTimeout(() => finish(new Error('알림 기능 준비 시간이 초과되었어요. 다시 시도해주세요.')), 15000);
    worker.addEventListener('statechange', changed);
    changed();
  });
}

export async function sendPushTest(delaySeconds) {
  const unsupported = pushSupportMessage();
  if (unsupported) throw new Error(unsupported);
  // Keep the permission prompt directly within the button's user gesture.
  const permission = Notification.permission === 'default'
    ? await Notification.requestPermission() : Notification.permission;
  if (permission !== 'granted') {
    throw new Error(permission === 'denied'
      ? '알림이 차단되어 있어요. 주소창의 사이트 설정에서 알림을 허용한 뒤 다시 눌러주세요.'
      : '알림 권한 요청을 닫았어요. 다시 눌러 알림을 허용해주세요.');
  }
  const { publicKey } = await request('/api/push/public-key');
  const applicationServerKey = decodeKey(publicKey);
  const registration = await navigator.serviceWorker.register(`${import.meta.env.BASE_URL}push-sw.js`, {
    scope: import.meta.env.BASE_URL,
  });
  await activeWorker(registration);
  let subscription = await registration.pushManager.getSubscription();
  if (subscription && !sameKey(subscription.options.applicationServerKey, applicationServerKey)) {
    await subscription.unsubscribe();
    subscription = null;
  }
  if (!subscription) subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey });
  const result = await postJsonWithCsrf('/api/push/tests', { subscription: subscription.toJSON(), delaySeconds });
  if (result.status === 'EXPIRED') await subscription.unsubscribe();
  return result;
}

export const getPushTest = (id) => request(`/api/push/tests/${encodeURIComponent(id)}`);

export async function resetExpiredSubscription() {
  const registration = await navigator.serviceWorker.getRegistration(import.meta.env.BASE_URL);
  const subscription = await registration?.pushManager.getSubscription();
  if (subscription) await subscription.unsubscribe();
}
