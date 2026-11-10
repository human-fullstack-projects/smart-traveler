import assert from 'node:assert/strict';
import { createServer } from 'vite';

const originalFetch = globalThis.fetch;
const calls = [];
globalThis.fetch = async (path, options = {}) => {
  calls.push({ path, options });
  if (path === '/api/auth/csrf') return Response.json({ headerName: 'X-CSRF-TOKEN', token: 'test-token' });
  if (path === '/api/human-courses') return Response.json([{ id: 7, title: '실제 코스' }]);
  if (path === '/api/human-courses/reservations/3') return Response.json({ id: 8 }, { status: 201 });
  if (path === '/api/human-courses/8' && options.method === 'DELETE') return new Response(null, { status: 204 });
  return Response.json({ message: '요청 실패' }, { status: 409 });
};

const server = await createServer({ server: { middlewareMode: true, hmr: false }, appType: 'custom' });
try {
  const api = await server.ssrLoadModule('/src/pages/human_course/api/humanCourses.js');
  assert.equal((await api.listCourses())[0].title, '실제 코스');
  assert.equal((await api.createCourse('3', { title: '새 글' })).id, 8);
  assert.equal(calls.at(-1).options.headers['X-CSRF-TOKEN'], 'test-token');
  assert.equal(calls.at(-1).options.method, 'POST');
  await api.deleteCourse(8);
  assert.equal(calls.at(-1).options.method, 'DELETE');
  await assert.rejects(api.editCourse(8, { title: '수정' }), /요청 실패/);
  console.log('Human course API: list, CSRF mutation, deletion, server error OK');
} finally {
  globalThis.fetch = originalFetch;
  await server.close();
}
