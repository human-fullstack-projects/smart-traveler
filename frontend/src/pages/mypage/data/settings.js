export const PACE_OPTIONS = [['relaxed', '여유롭게'], ['normal', '보통'], ['packed', '밀도있게']];
export const WAKE_OPTIONS = [['early', '새벽'], ['normal', '오전'], ['late', '오후']];
export const TAG_GROUPS = [['activityTime', '선호 활동시간대'], ['environment', '선호 환경'], ['companion', '선호 동행'], ['transport', '선호 이동수단'], ['budget', '여행 경비'], ['style', '여행 스타일'], ['region', '지역취향'], ['interest', '여행취향'], ['food', '음식취향']];
export const TRAVEL_TASTES = ['맛집탐방', '카페투어', '자연풍경', '문화예술', '역사탐방', '액티비티', '휴양힐링', '쇼핑투어', '공예체험', '농촌·전원', '도시투어', '호캉스', '사진·감성', '야경감상'].map(name => ({ type: 'interest', name }));
export const FOOD_TASTES = ['한식', '중식', '일식', '양식', '동남아음식', '분식', '육류', '해산물', '채식', '디저트'].map(name => ({ type: 'food', name }));
export const TRANSPORT_OPTIONS = ['대중교통', '자동차', '도보', '자전거'].map(name => ({ type: 'transport', name }));
export const TRAVEL_STYLES = ['휴식파', '탐험파', '가이드파', '자유파'].map(name => ({ type: 'style', name }));
export const REGION_TASTES = ['도시', '농촌', '호수·강', '섬', '산·숲', '바다·해변'].map(name => ({ type: 'style', name }));
export const COMPANION_OPTIONS = ['혼자', '연인', '부부', '가족(아이동반)', '부모님', '조부모님', '친구', '반려동물'].map(name => ({ type: 'style', name }));
export const BUDGET_OPTIONS = ['저예산', '가성비', '럭셔리', '프리미엄'].map(name => ({ type: 'style', name }));
export const ENVIRONMENT_OPTIONS = ['실내', '야외'].map(name => ({ type: 'style', name }));
export const ACTIVITY_TIME_OPTIONS = ['낮', '밤'].map(name => ({ type: 'style', name }));
export const CHECKBOX_TASTES = { interest: TRAVEL_TASTES, region: REGION_TASTES, food: FOOD_TASTES, transport: TRANSPORT_OPTIONS, companion: COMPANION_OPTIONS, style: TRAVEL_STYLES, budget: BUDGET_OPTIONS, environment: ENVIRONMENT_OPTIONS, activityTime: ACTIVITY_TIME_OPTIONS };
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
  '취향을 바꿔도 기존 예약·확정 코스에는 적용되지 않습니다.',
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
  if (!PACE_OPTIONS.some(([code]) => code === form.pace) || !WAKE_OPTIONS.some(([code]) => code === form.wakeUp)) return '일정 강도와 여행시작 시간대를 선택해 주세요.';
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
