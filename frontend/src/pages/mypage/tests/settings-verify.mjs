// frontend에서 node src/pages/mypage/tests/settings-verify.mjs (빌드 없음)
import assert from 'node:assert/strict';
import { profileError, preferenceError, tagKey, preferenceNotices, SURVEY_REGIONS, isSurveyTag, normalTags, surveyDone, surveyError } from '../data/settings.js';
import { routeAccess } from '../data/store.js';

const profile = { nickname: '여행자', email: 'traveler@example.invalid', changePassword: false,
  currentPassword: '', newPassword: '', passwordConfirmation: '' };
assert.equal(profileError(profile), '');
assert.ok(profileError({ ...profile, nickname: ' ' }));
assert.ok(profileError({ ...profile, nickname: '가'.repeat(51) }));
assert.ok(profileError({ ...profile, email: 'not-email' }));
const change = { ...profile, changePassword: true, currentPassword: 'Old12345', newPassword: 'Travel12345', passwordConfirmation: 'Travel12345' };
assert.equal(profileError(change), '');
for (const invalid of [
  { currentPassword: '' }, { newPassword: 'short1' }, { newPassword: 'passwordonly' },
  { passwordConfirmation: 'different' }, { newPassword: 'Old12345', passwordConfirmation: 'Old12345' },
  { newPassword: '한'.repeat(24) + 'A1', passwordConfirmation: '한'.repeat(24) + 'A1' },
]) assert.ok(profileError({ ...change, ...invalid }));
assert.equal(profileError({ ...change, changePassword: false, newPassword: 'bad' }), '', '변경 해제 시 비밀번호 필드는 무시');

const catalog = [{ type: 'interest', name: '자연' }, { type: 'food', name: '한식' }, { type: 'transport', name: '도보' }, { type: 'style', name: '휴식' }];
const selected = catalog.map((option, i) => ({ ...option, polarity: i % 2 ? 'avoid' : 'like', weight: i % 3 + 1 }));
const preferences = { pace: 'relaxed', wakeUp: 'early', tags: selected };
assert.equal(preferenceError(preferences, catalog), '');
assert.equal(preferenceError({ pace: 'normal', wakeUp: 'late', tags: [] }, catalog), '', '태그 전체 해제 가능');
for (const invalid of [
  { pace: 'unknown' }, { wakeUp: 'unknown' }, { tags: [...selected, selected[0]] },
  { tags: [{ ...selected[0], weight: 0 }] }, { tags: [{ ...selected[0], weight: 4 }] },
  { tags: [{ ...selected[0], weight: 1.5 }] }, { tags: [{ ...selected[0], polarity: 'other' }] },
  { tags: [{ ...selected[0], name: '임의 태그' }] },
]) assert.ok(preferenceError({ ...preferences, ...invalid }, catalog));
assert.notEqual(tagKey(catalog[0]), tagKey({ ...catalog[0], type: 'style' }), '동명 태그도 분류별 구분');
assert.equal(preferenceNotices.length, 2);
const surveyCatalog = [...catalog, { type: 'style', name: '지역:경주' }, { type: 'style', name: '분위기:조용한 여행' }];
const regionTag = name => ({ type: 'style', name: `지역:${name}`, polarity: 'like', weight: 1 });
const moodTag = name => ({ type: 'style', name: `분위기:${name}`, polarity: 'like', weight: 1 });
const surveyTags = [regionTag('경주'), regionTag('전주'), regionTag('제주'), moodTag('조용한 여행')];
assert.equal(SURVEY_REGIONS.length, 20);
assert.equal(isSurveyTag(surveyTags[0]), true);
assert.equal(isSurveyTag(catalog[3]), false);
assert.equal(surveyDone(surveyTags), true);
assert.equal(surveyDone([...surveyTags.slice(0, 3), { ...surveyTags[3], weight: 2 }]), false);
assert.equal(surveyDone([...surveyTags, regionTag('거제')]), false);
assert.equal(surveyDone([surveyTags[0], surveyTags[0], surveyTags[2], surveyTags[3]]), false);
assert.equal(surveyError(['경주', '전주', '제주'], '조용한 여행'), '');
assert.ok(surveyError(['경주', '전주', '전주'], '조용한 여행'));
assert.ok(surveyError(['경주', '전주', '제주'], ''));
assert.ok(surveyError(['경주', '전주', '없는 곳'], '활발한 여행'));
assert.deepEqual(normalTags([...selected, ...surveyTags]), selected, '일반 폼 태그에서 조사 태그 제외');
assert.deepEqual(normalTags(surveyCatalog), catalog, '일반 카탈로그에서 조사 태그 제외');
assert.equal(preferenceError({ ...preferences, tags: normalTags([...selected, ...surveyTags]) }, normalTags(surveyCatalog)), '', '일반 저장 검증 통과');
for (const path of ['/my-profile', '/my-travel-style']) {
  assert.equal(routeAccess(path, { member: null, authChecked: true, authCheckFailed: false }), 'login');
  assert.equal(routeAccess(path, { member: { memberId: 7 }, authChecked: true, authCheckFailed: false }), 'allowed');
}
console.log('PASS: 프로필·비밀번호 검증, 일반 취향과 조사 태그 분리, 조사 완료·입력 검증, 회원 전용 접근');
