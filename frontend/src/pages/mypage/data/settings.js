export const PACE_OPTIONS = [['relaxed', '여유'], ['normal', '보통'], ['packed', '빽빽']];
export const WAKE_OPTIONS = [['early', '이른'], ['normal', '보통'], ['late', '늦은']];
export const TAG_GROUPS = [['interest', '관심사'], ['food', '음식'], ['transport', '교통'], ['style', '여행 스타일']];
export const tagKey = tag => `${tag.type}:${tag.name}`;
export const SURVEY_REGIONS = ['명동', '홍대', '강남', '잠실', '해운대', '광안리', '송도', '경주', '포항', '속초', '춘천', '여수', '전주', '군산', '제주', '서귀포', '거제', '통영', '담양', '태안'];
export const SURVEY_MOODS = ['조용한 여행', '활발한 여행'];
export const isSurveyTag = tag => tag?.type === 'style' && (tag.name?.startsWith('지역:') || tag.name?.startsWith('분위기:'));
export const normalTags = tags => (tags || []).filter(tag => !isSurveyTag(tag));
export function surveyDone(tags) {
  const chosen = (tags || []).filter(tag => isSurveyTag(tag) && tag.polarity === 'like' && tag.weight === 1);
  return chosen.filter(tag => tag.name.startsWith('지역:')).length === 3
    && chosen.filter(tag => tag.name.startsWith('분위기:')).length === 1
    && new Set(chosen.map(tagKey)).size === 4;
}
export function surveyError(regions, mood) {
  if (!Array.isArray(regions) || regions.length !== 3 || new Set(regions).size !== 3 || regions.some(name => !SURVEY_REGIONS.includes(name))) return '가고 싶은 지역을 3곳 골라주세요.';
  if (!SURVEY_MOODS.includes(mood)) return '여행 분위기를 하나 골라주세요.';
  return '';
}
export const preferenceNotices = [
  '개별 여행에서 선택한 조건이 기본 취향보다 우선합니다.',
  '취향을 바꿔도 기존 예약·확정 조건·코스에는 자동으로 적용되지 않습니다.',
];

export function profileError(form) {
  if (!form.nickname.trim() || form.nickname.trim().length > 50) return '닉네임은 1~50자로 입력해 주세요.';
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim()) || form.email.trim().length > 255) return '올바른 이메일을 입력해 주세요.';
  if (form.changePassword) {
    if (!form.currentPassword) return '현재 비밀번호를 입력해 주세요.';
    if (form.newPassword.length < 8 || new TextEncoder().encode(form.newPassword).length > 72
      || !/[A-Za-z]/.test(form.newPassword) || !/[0-9]/.test(form.newPassword)) return '새 비밀번호는 영문·숫자 포함 8자 이상, UTF-8 기준 72바이트 이하로 입력해 주세요.';
    if (form.newPassword !== form.passwordConfirmation) return '새 비밀번호 확인이 일치하지 않습니다.';
    if (form.currentPassword === form.newPassword) return '현재 비밀번호와 다른 비밀번호를 입력해 주세요.';
  }
  return '';
}

export function preferenceError(form, catalog) {
  if (!PACE_OPTIONS.some(([code]) => code === form.pace) || !WAKE_OPTIONS.some(([code]) => code === form.wakeUp)) return '일정 강도와 기상 시간대를 선택해 주세요.';
  if (!Array.isArray(form.tags) || form.tags.length > 80) return '선택한 태그를 확인해 주세요.';
  const allowed = new Set(catalog.map(tagKey));
  const selected = new Set();
  for (const tag of form.tags) {
    const key = tagKey(tag);
    if (!allowed.has(key) || selected.has(key) || !['like', 'avoid'].includes(tag.polarity)
      || !Number.isInteger(tag.weight) || tag.weight < 1 || tag.weight > 3) return '태그의 중복·선호 여부·강도(1~3)를 확인해 주세요.';
    selected.add(key);
  }
  return '';
}
