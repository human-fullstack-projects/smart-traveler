// A notification-only worker: no page cache or fetch interception.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

self.addEventListener('push', (event) => {
  let payload = {};
  try { payload = event.data?.json() || {}; } catch { /* Show a fallback for unexpected payloads. */ }
  event.waitUntil(self.registration.showNotification(payload.title || '스마트 트래블 알림', {
    body: payload.body || '새 알림이 도착했어요.',
    tag: payload.tag || 'smart-traveler-test',
    data: { url: `${self.registration.scope}#/` },
  }));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const destination = `${self.registration.scope}#/`;
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    const existing = windows.find((client) => client.url.startsWith(self.registration.scope));
    if (existing) {
      await existing.navigate(destination);
      return existing.focus();
    }
    return self.clients.openWindow(destination);
  })());
});
