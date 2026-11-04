// 서버의 RecommendationRequest 필드와 같은 이름을 사용한다.
export function createEmptyConditions() {
  return {
    regionId: null,
    regionName: '',
    startDate: '',
    durationDays: null,
    adults: null,
    children: null,
    companionType: '',
    transportType: '',
    preferences: [],
    fixedEvents: [],
    flowType: 'COURSE_FIRST',
    lodgingSelection: null,
  };
}

// 숫자 입력을 비웠을 때 0으로 바뀌지 않도록 구분한다.
export function numberOrNull(value) {
  return value === '' ? null : Number(value);
}

export function getConditionErrors(conditions) {
  const errors = [];
  const isIntegerAtLeast = (value, min) => Number.isSafeInteger(value) && value >= min;
  const validDate = (value) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const parsed = new Date(`${value}T00:00:00Z`);
    return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
  };

  if (!isIntegerAtLeast(conditions.regionId, 1)) errors.push('지역 번호를 입력해 주세요.');
  if (!validDate(conditions.startDate)) errors.push('출발 날짜를 입력해 주세요.');
  if (!isIntegerAtLeast(conditions.durationDays, 1) || conditions.durationDays > 7) errors.push('여행 기간은 1일에서 7일 사이로 입력해 주세요.');
  if (!isIntegerAtLeast(conditions.adults, 1)) errors.push('성인 인원은 1명 이상 입력해 주세요.');
  if (!isIntegerAtLeast(conditions.children, 0)) errors.push('아동 인원을 입력해 주세요.');
  if (!conditions.companionType.trim()) errors.push('동행 유형을 선택해 주세요.');
  if (!conditions.transportType.trim()) errors.push('교통수단을 선택해 주세요.');
  if (conditions.preferences.length > 20 || conditions.preferences.some((item) => item.length > 100)) errors.push('여행 취향은 20개 이하, 각 100자 이하로 입력해 주세요.');
  if (conditions.fixedEvents.length > 30) errors.push('고정 일정은 30개 이하로 입력해 주세요.');

  const lastDate = validDate(conditions.startDate) && isIntegerAtLeast(conditions.durationDays, 1) && conditions.durationDays <= 7
    ? new Date(Date.parse(`${conditions.startDate}T00:00:00Z`) + (conditions.durationDays - 1) * 86400000).toISOString().slice(0, 10)
    : null;
  conditions.fixedEvents.forEach((event, index) => {
    if (!validDate(event.date) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(event.startTime) || !event.placeName.trim() || event.placeName.trim().length > 200) {
      errors.push(`고정 일정 ${index + 1}의 날짜, 시간, 장소를 확인해 주세요.`);
    } else if (lastDate && (event.date < conditions.startDate || event.date > lastDate)) {
      errors.push(`고정 일정 ${index + 1}은 여행 기간 안에 있어야 합니다.`);
    }
  });

  if (conditions.flowType === 'STAY_FIRST') {
    const stay = conditions.lodgingSelection;
    if (!stay || !isIntegerAtLeast(stay.lodgingId, 1) || !isIntegerAtLeast(stay.roomId, 1) || !validDate(stay.checkIn) || !validDate(stay.checkOut) || stay.checkOut <= stay.checkIn) {
      errors.push('숙소·객실 번호와 체크인·체크아웃 날짜를 확인해 주세요.');
    }
  }
  return errors;
}

// 화면에서 작성한 값만 서버 형식으로 정리한다. 이 함수는 서버에 전송하지 않는다.
export function toRecommendationRequest(conditions) {
  return {
    regionId: conditions.regionId,
    startDate: conditions.startDate,
    durationDays: conditions.durationDays,
    adults: conditions.adults,
    children: conditions.children,
    companionType: conditions.companionType,
    transportType: conditions.transportType,
    preferences: conditions.preferences.map((item) => item.trim()),
    fixedEvents: conditions.fixedEvents.map(({ date, startTime, placeName }) => ({ date, startTime, placeName: placeName.trim() })),
    flowType: conditions.flowType,
    lodgingSelection: conditions.flowType === 'STAY_FIRST' ? conditions.lodgingSelection : null,
  };
}
