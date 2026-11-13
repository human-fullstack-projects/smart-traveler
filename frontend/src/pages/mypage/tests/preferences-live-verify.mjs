// Opt-in integration check against the running local frontend/backend and real DB.
// Creates two dedicated USER accounts; never changes an existing user's preferences.
// Run from frontend: node src/pages/mypage/tests/preferences-live-verify.mjs
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
import { createServer } from 'vite';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { surveyDone } from '../data/settings.js';

const base = process.env.PREFERENCE_TEST_URL || 'http://127.0.0.1:5173';
assert.ok(['127.0.0.1', 'localhost'].includes(new URL(base).hostname), 'Local test server only');
class Session {
  cookies = new Map();
  async request(path, { method = 'GET', body, headers = {} } = {}) {
    const response = await fetch(`${base}${path}`, {
      method, redirect: 'manual',
      headers: { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; '), ...headers },
      body,
    });
    for (const cookie of response.headers.getSetCookie()) {
      const pair = cookie.split(';')[0];
      const separator = pair.indexOf('=');
      this.cookies.set(pair.slice(0, separator), pair.slice(separator + 1));
    }
    const text = await response.text();
    let data;
    try { data = JSON.parse(text); } catch { data = null; }
    return { status: response.status, data };
  }
  async mutate(path, method, payload, form = false) {
    const csrf = await this.request('/api/auth/csrf');
    assert.equal(csrf.status, 200);
    return this.request(path, { method,
      headers: { [csrf.data.headerName]: csrf.data.token, 'Content-Type': form ? 'application/x-www-form-urlencoded' : 'application/json' },
      body: form ? new URLSearchParams(payload).toString() : JSON.stringify(payload),
    });
  }
  async login(account) {
    const login = await this.mutate('/api/auth/login', 'POST', account, true);
    assert.ok(login.status >= 200 && login.status < 400, `Login failed: ${login.status}`);
    const me = await this.request('/api/auth/me');
    assert.equal(me.status, 200);
    return me.data;
  }
  async logout() {
    const result = await this.mutate('/api/auth/logout', 'POST', {}, true);
    assert.ok(result.status >= 200 && result.status < 400);
    assert.equal((await this.request('/api/mypage/preferences')).status, 401);
  }
}
const suffix = `${Date.now()}_${randomBytes(3).toString('hex')}`;
const accounts = ['a', 'b'].map(name => ({ loginId: `pref_e2e_${suffix}_${name}`, password: `Test9${randomBytes(12).toString('hex')}` }));
const session = new Session();
const terms = await session.request('/api/auth/terms');
assert.equal(terms.status, 200);
const members = [];
for (const account of accounts) {
  const result = await session.mutate('/api/auth/signup', 'POST', {
    ...account, role: 'USER', nickname: `Preference test ${account.loginId.slice(-8)}`,
    email: `${account.loginId}@example.invalid`,
    consents: terms.data.map(term => ({ termsId: term.termsId, agreed: Boolean(term.required) })),
  });
  assert.equal(result.status, 201, `Signup failed: ${JSON.stringify(result.data)}`);
  members.push(result.data.memberId);
}
const tag = name => ({ type: 'style', name, polarity: 'like', weight: 1 });
const initial = { pace: 'packed', wakeUp: 'normal', tags: [tag('장소:실내'), tag('선호지역:시골'), tag('선호지역:바다')], surveyOnly: true };
const edited = { pace: 'relaxed', wakeUp: 'normal', tags: [tag('장소:둘 다'), tag('선호지역:도시'), tag('선호지역:섬'), tag('선호지역:산·숲')], surveyOnly: true };
const other = { pace: 'normal', wakeUp: 'normal', tags: [tag('장소:야외'), tag('선호지역:호수·강')], surveyOnly: true };
async function read() {
  const result = await session.request('/api/mypage/preferences');
  assert.equal(result.status, 200);
  return result.data;
}
async function save(memberId, preferences) {
  const result = await session.mutate('/api/mypage/preferences', 'PUT', { expectedMemberId: memberId, ...preferences });
  assert.equal(result.status, 200, `Save failed: ${JSON.stringify(result.data)}`);
}
function check(data, memberId, expected) {
  assert.equal(data.memberId, memberId);
  assert.equal(data.saved, true);
  assert.equal(data.pace, expected.pace);
  assert.equal(surveyDone(data.tags), true);
  assert.deepEqual(data.tags.map(item => item.name).sort(), expected.tags.map(item => item.name).sort());
}
let vite;
try {
  await session.login(accounts[0]);
  assert.equal(surveyDone((await read()).tags), false, 'First login requires survey');
  assert.equal((await session.mutate('/api/mypage/preferences', 'PUT', { ...initial, expectedMemberId: members[0], tags: [] })).status, 400, 'Incomplete survey cannot be saved');
  await save(members[0], initial);
  await session.logout();
  await session.login(accounts[0]);
  const firstRead = await read();
  check(firstRead, members[0], initial);
  console.log('PASS: first login survey -> DB save -> logout -> login -> GET persisted preferences');

  // Render the actual My Travel Style component using real API responses.
  vite = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
  const { default: TravelStyleWizard } = await vite.ssrLoadModule('/src/pages/mypage/preferences/TravelStyleWizard.jsx');
  function checkScreen(data, selectedLabels) {
    const html = renderToStaticMarkup(React.createElement(TravelStyleWizard, { member: { memberId: data.memberId }, initialData: data }));
    const buttons = [...html.matchAll(/<button\b[^>]*aria-pressed="true"[^>]*>([\s\S]*?)<\/button>/g)];
    const labels = buttons.map(match => match[1].match(/<strong>(.*?)<\/strong>/)?.[1]);
    assert.deepEqual(labels.sort(), [...selectedLabels].sort(), 'Saved values must appear selected in My Travel Style');
    assert.equal(html.includes('나중에 하기'), false);
  }
  checkScreen(firstRead, ['알차게 여행', '실내', '시골', '바다']);
  console.log('PASS: real saved data renders selected in My Travel Style; no skip button');
  await save(members[0], edited);
  await session.logout();
  await session.login(accounts[0]);
  const updatedRead = await read();
  check(updatedRead, members[0], edited);
  checkScreen(updatedRead, ['여유로운 여행', '둘 다', '도시', '섬', '산·숲']);
  console.log('PASS: edit -> DB save -> logout -> login -> updated selections render correctly');
  assert.equal((await session.request('/api/mypage/preferences', { headers: { 'X-MyPage-Member': String(members[1]) } })).status, 403);
  assert.equal((await session.mutate('/api/mypage/preferences', 'PUT', { ...other, expectedMemberId: members[1] })).status, 403);
  await session.logout();
  await session.login(accounts[1]);
  const blank = await read();
  assert.equal(blank.memberId, members[1]);
  assert.equal(surveyDone(blank.tags), false, 'Other account must not inherit first account preferences');
  await save(members[1], other);
  await session.logout();
  await session.login(accounts[1]);
  const otherRead = await read();
  check(otherRead, members[1], other);
  checkScreen(otherRead, ['적당한 여행', '야외', '호수·강']);
  await session.logout();
  await session.login(accounts[0]);
  check(await read(), members[0], edited);
  await session.logout();
  console.log('PASS: two real accounts remain isolated; mismatched member read/write returns 403');
  console.log(JSON.stringify({ databaseVerificationMemberIds: members, testAccounts: accounts.map(account => account.loginId) }));
} finally {
  if (vite) await vite.close();
}
