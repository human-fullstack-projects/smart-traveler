// Requires the running local app and test_fixtures_user1.sql in the local DB.
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

const base = 'http://127.0.0.1:5173';
const cookies = new Map();
async function request(path, options = {}) {
  const response = await fetch(base + path, { ...options, redirect: 'manual', headers: {
    Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; '), ...options.headers,
  } });
  for (const cookie of response.headers.getSetCookie()) {
    const pair = cookie.split(';')[0];
    const separator = pair.indexOf('=');
    cookies.set(pair.slice(0, separator), pair.slice(separator + 1));
  }
  const text = await response.text();
  return { status: response.status, data: text ? JSON.parse(text) : null };
}
async function post(path, values) {
  const csrf = (await request('/api/auth/csrf')).data;
  return request(path, { method: 'POST', headers: { [csrf.headerName]: csrf.token, 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams(values) });
}
async function login(loginId) {
  const result = await post('/api/auth/login', { loginId, password: process.env.LOCAL_TEST_PASSWORD || '123456' });
  assert.ok(result.status >= 200 && result.status < 400);
}
let vite;
try {
  await login('user1');
  const me = await request('/api/auth/me');
  const result = await request('/api/mypage');
  assert.equal(result.status, 200);
  const { courses, bookings, posts } = result.data;
  const testCourses = courses.filter(course => course.title.startsWith('[테스트]'));
  const testBookings = bookings.filter(booking => booking.reservationNo.startsWith('TEST-MYPAGE-USER1-20261113-'));
  const testPosts = posts.filter(item => item.title.startsWith('[테스트]'));
  for (const items of [testCourses, testBookings, testPosts]) assert.equal(items.length, 3);
  for (const course of testCourses) {
    const detail = await request(`/api/mypage/courses/${course.id}`);
    assert.equal(detail.status, 200);
    assert.equal(detail.data.days.length, 2);
    assert.equal(detail.data.days.flatMap(day => day.places).length, 5);
  }
  for (const booking of testBookings) {
    assert.ok(testCourses.some(course => course.id === booking.courseId));
    assert.ok(booking.stay.startsWith('[테스트]'));
    assert.equal((await request(`/api/mypage/reservations/${booking.id}`)).status, 200);
  }
  for (const item of testPosts) {
    assert.ok(testBookings.some(booking => booking.id === item.reservationId));
    assert.equal((await request(`/api/mypage/posts/${item.id}`)).status, 200);
    const publicPost = await request(`/api/human-courses/${item.id}`);
    assert.equal(publicPost.status, 200);
    assert.equal(publicPost.data.mine, true);
    assert.equal(publicPost.data.plan.length, 2);
    const csrf = (await request('/api/auth/csrf')).data;
    const saved = await request(`/api/human-courses/${item.id}`, {
      method: 'PUT', headers: { [csrf.headerName]: csrf.token, 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: publicPost.data.title, category: publicPost.data.category,
        companion: publicPost.data.companion, summary: publicPost.data.summary,
        body: publicPost.data.body, photoAssets: [], replacePhotos: false }),
    });
    assert.equal(saved.status, 200, 'Owner can save edits to the test recommendation');
  }
  const eligibility = await request('/api/human-courses/eligibility');
  assert.equal(eligibility.status, 200);
  for (const booking of testBookings) {
    const item = eligibility.data.find(row => row.id === booking.id);
    assert.equal(item.itineraryFinal, true);
    assert.ok(item.existingPostId);
    assert.ok(item.eligibilityError.includes('이미 작성한'), 'One recommendation per reservation');
  }
  vite = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
  for (const [name, props, expected] of [
    ['MyCourses', { courses, member: me.data, isGuest: false }, testCourses.map(item => item.title)],
    ['Reservations', { bookings }, testBookings.map(item => item.reservationNo)],
    ['MyPosts', { posts }, testPosts.map(item => item.title)],
  ]) {
    const { default: Page } = await vite.ssrLoadModule(`/src/pages/mypage/pages/${name}.jsx`);
    const html = renderToStaticMarkup(React.createElement(Page, props));
    for (const text of expected) assert.ok(html.includes(text), `${name}: missing ${text}`);
    console.log(`PASS: ${name} renders 3 marked DB fixtures using real API data`);
  }
  await post('/api/auth/logout', {});
  await login('user2');
  const other = await request('/api/mypage');
  assert.equal(other.status, 200);
  assert.ok(!other.data.courses.some(item => testCourses.some(course => course.id === item.id)));
  assert.ok(!other.data.bookings.some(item => testBookings.some(booking => booking.id === item.id)));
  assert.ok(!other.data.posts.some(item => testPosts.some(post => post.id === item.id)));
  assert.equal((await request(`/api/mypage/courses/${testCourses[0].id}`)).status, 404);
  await post('/api/auth/logout', {});
  console.log('PASS: course details, booking/post links, public post details, edit API, account isolation');
} finally {
  if (vite) await vite.close();
}
