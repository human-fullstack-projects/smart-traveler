// frontend에서 node src/pages/mypage/tests/settings-verify.mjs (빌드 없음)
import assert from 'node:assert/strict';
import { profileError, preferenceError, tagKey, SURVEY_PLACES, SURVEY_AREAS, isSurveyTag, normalTags, surveyDone, surveyError } from '../data/settings.js';
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
const surveyCatalog = [...catalog, { type: 'style', name: '지역:경주' }, { type: 'style', name: '분위기:조용한 여행' }, { type: 'style', name: '장소:야외' }, { type: 'style', name: '선호지역:도시' }];
const placeTag = name => ({ type: 'style', name: `장소:${name}`, polarity: 'like', weight: 1 });
const areaTag = name => ({ type: 'style', name: `선호지역:${name}`, polarity: 'like', weight: 1 });
const surveyTags = [placeTag('야외'), areaTag('도시'), areaTag('바다')];
assert.equal(SURVEY_PLACES.length, 3);
assert.equal(SURVEY_AREAS.length, 6);
assert.equal(isSurveyTag(surveyTags[0]), true);
assert.equal(isSurveyTag(catalog[3]), false);
assert.equal(surveyDone(surveyTags), true);
assert.equal(surveyDone([placeTag('실내'), areaTag('산·숲')]), true);
assert.equal(surveyDone([{ ...surveyTags[0], weight: 2 }, ...surveyTags.slice(1)]), false);
assert.equal(surveyDone([...surveyTags, placeTag('실내')]), false);
assert.equal(surveyDone([...surveyTags, areaTag('섬'), areaTag('시골')]), false);
assert.equal(surveyDone([surveyTags[0], surveyTags[1], surveyTags[1]]), false);
assert.equal(surveyDone([{ type: 'style', name: '지역:경주', polarity: 'like', weight: 1 }]), false, '옛 응답은 새 조사를 완료하지 않음');
assert.equal(surveyError('packed', '야외', ['도시']), '');
assert.equal(surveyError('relaxed', '둘 다', ['도시', '바다', '산·숲']), '');
assert.ok(surveyError('', '야외', ['도시']));
assert.ok(surveyError('normal', '', ['도시']));
assert.ok(surveyError('normal', '야외', []));
assert.ok(surveyError('normal', '야외', ['도시', '도시']));
assert.ok(surveyError('normal', '야외', ['도시', '시골', '섬', '바다']));
assert.ok(surveyError('normal', '야외', ['없는 곳']));
assert.deepEqual(normalTags([...selected, ...surveyTags]), selected, '일반 폼 태그에서 조사 태그 제외');
assert.deepEqual(normalTags(surveyCatalog), catalog, '일반 카탈로그에서 조사 태그 제외');
assert.equal(preferenceError({ ...preferences, tags: normalTags([...selected, ...surveyTags]) }, normalTags(surveyCatalog)), '', '일반 저장 검증 통과');
for (const path of ['/my-profile', '/my-travel-style']) {
  assert.equal(routeAccess(path, { member: null, authChecked: true, authCheckFailed: false }), 'login');
  assert.equal(routeAccess(path, { member: { memberId: 7 }, authChecked: true, authCheckFailed: false }), 'allowed');
}
console.log('PASS: 프로필·비밀번호 검증, 일반 취향과 조사 태그 분리, 조사 완료·입력 검증, 회원 전용 접근');
