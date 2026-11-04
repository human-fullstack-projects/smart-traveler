import { useEffect, useRef, useState } from 'react';
import { getConditionErrors, numberOrNull, toRecommendationRequest } from './travelConditions.js';

const companions = [ ['SOLO', '혼자'], ['COUPLE', '연인'], ['FRIENDS', '친구'], ['FAMILY', '가족'] ];
const transports = [ ['PUBLIC_TRANSPORT', '대중교통'], ['CAR', '자동차'] ];
const labelFor = (list, value) => list.find(([code]) => code === value)?.[1] || '정해지지 않음';

/** 사용자가 입력한 조건과 확인 상태를 대화에 연결한다. */
export default function TravelConditions({ conditions, confirmed, onChange, onConfirm }) {
  const [open, setOpen] = useState(false);
  const [preferencesText, setPreferencesText] = useState(conditions.preferences.join(', '));
  const [errors, setErrors] = useState([]);
  const preferenceInput = useRef(null);
  useEffect(() => {
    if (document.activeElement !== preferenceInput.current) setPreferencesText(conditions.preferences.join(', '));
  }, [conditions.preferences]);

  function update(name, value) {
    onChange({ ...conditions, [name]: value });
    setErrors([]);
  }

  function updateStay(name, value) {
    const current = conditions.lodgingSelection || { lodgingId: null, roomId: null, checkIn: '', checkOut: '' };
    update('lodgingSelection', { ...current, [name]: value });
  }

  function updateEvent(index, name, value) {
    update('fixedEvents', conditions.fixedEvents.map((event, eventIndex) => eventIndex === index ? { ...event, [name]: value } : event));
  }

  function confirm(event) {
    event.preventDefault();
    const request = toRecommendationRequest(conditions);
    // 지역명은 먼저 확인하고 실제 지역 번호 매핑은 추후 연결한다.
    const nextErrors = getConditionErrors(request).filter((error) => !(conditions.regionName?.trim() && error === '지역 번호를 입력해 주세요.'));
    setErrors(nextErrors);
    if (nextErrors.length === 0) {
      onConfirm();
      setOpen(false);
    }
  }

  return <aside className="travel-conditions" aria-labelledby="travel-conditions-title">
    <div className="travel-conditions-heading">
      <div><span className="greeting-label">YOUR TRAVEL PLAN</span><h2 id="travel-conditions-title">여행 조건</h2></div>
      <span className={`condition-status${confirmed ? ' confirmed' : ''}`}>{confirmed ? '입력 확인' : '확인 전'}</span>
    </div>
    <p className="travel-conditions-description">여행 조건을 확인하고 직접 수정할 수 있어요.</p>
    <dl className="travel-condition-summary">
      <div><dt>지역</dt><dd>{conditions.regionName || (conditions.regionId ? `지역 번호 ${conditions.regionId}` : '정해지지 않음')}</dd></div>
      <div><dt>출발</dt><dd>{conditions.startDate || '정해지지 않음'}</dd></div>
      <div><dt>기간</dt><dd>{conditions.durationDays ? `${conditions.durationDays}일` : '정해지지 않음'}</dd></div>
      <div><dt>인원</dt><dd>{conditions.adults === null || conditions.children === null ? '정해지지 않음' : `성인 ${conditions.adults}명 · 아동 ${conditions.children}명`}</dd></div>
      <div><dt>동행</dt><dd>{labelFor(companions, conditions.companionType)}</dd></div>
      <div><dt>교통</dt><dd>{labelFor(transports, conditions.transportType)}</dd></div>
      <div><dt>취향</dt><dd>{conditions.preferences.length ? conditions.preferences.join(', ') : '선택 사항'}</dd></div>
      <div><dt>고정 일정</dt><dd>{conditions.fixedEvents.length ? conditions.fixedEvents.map((event, index) => <span className="condition-summary-line" key={index}>{event.date || '날짜 미정'} {event.startTime || '시간 미정'} · {event.placeName || '장소 미정'}</span>) : '없음'}</dd></div>
      <div><dt>진행 방식</dt><dd>{conditions.flowType === 'STAY_FIRST' ? '숙소 먼저' : '코스 먼저'}</dd></div>
      {conditions.flowType === 'STAY_FIRST' && <><div><dt>숙소·객실</dt><dd>{conditions.lodgingSelection?.lodgingId && conditions.lodgingSelection?.roomId ? `숙소 ${conditions.lodgingSelection.lodgingId} · 객실 ${conditions.lodgingSelection.roomId}` : '선택 필요'}</dd></div><div><dt>숙박 날짜</dt><dd>{conditions.lodgingSelection?.checkIn && conditions.lodgingSelection?.checkOut ? `${conditions.lodgingSelection.checkIn} ~ ${conditions.lodgingSelection.checkOut}` : '선택 필요'}</dd></div></>}
    </dl>
    <button className="conditions-toggle" type="button" aria-expanded={open} aria-controls="travel-condition-form" onClick={() => setOpen(!open)}>{open ? '접기' : '조건 직접 입력'} <span aria-hidden="true">{open ? '−' : '+'}</span></button>
    <p className="conditions-note">대화에서 찾은 지역은 이름으로 저장됩니다. 실제 지역 연결과 일정 생성은 준비 중이에요.</p>
    {open && <form id="travel-condition-form" className="travel-condition-form" onSubmit={confirm} noValidate>
      <label>지역 이름<input value={conditions.regionName ?? ''} maxLength="100" onChange={(event) => { onChange({ ...conditions, regionName: event.target.value, regionId: null }); setErrors([]); }} placeholder="예: 부산" /></label>
      <p className="conditions-field-note">입력한 지역의 실제 존재 여부와 지역 번호는 이후 연결합니다.</p>
      <div className="condition-fields">
        <label>출발 날짜<input type="date" value={conditions.startDate} onChange={(event) => update('startDate', event.target.value)} /></label>
        <label>여행 일수<input type="number" min="1" max="7" step="1" value={conditions.durationDays ?? ''} onChange={(event) => update('durationDays', numberOrNull(event.target.value))} /></label>
        <label>성인<input type="number" min="1" step="1" value={conditions.adults ?? ''} onChange={(event) => update('adults', numberOrNull(event.target.value))} /></label>
        <label>아동<input type="number" min="0" step="1" value={conditions.children ?? ''} onChange={(event) => update('children', numberOrNull(event.target.value))} /></label>
        <label>동행 유형<select value={conditions.companionType} onChange={(event) => update('companionType', event.target.value)}><option value="">선택해 주세요</option>{companions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label>교통수단<select value={conditions.transportType} onChange={(event) => update('transportType', event.target.value)}><option value="">선택해 주세요</option>{transports.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      </div>
      <label>여행 취향 <span className="optional-label">선택</span><input ref={preferenceInput} value={preferencesText} placeholder="예: 바다, 카페" onChange={(event) => { const text = event.target.value; setPreferencesText(text); update('preferences', text.split(',').map((item) => item.trim()).filter(Boolean)); }} /></label>
      <label>진행 방식<select value={conditions.flowType} onChange={(event) => { const flowType = event.target.value; onChange({ ...conditions, flowType, lodgingSelection: flowType === 'STAY_FIRST' ? { lodgingId: null, roomId: null, checkIn: '', checkOut: '' } : null }); setErrors([]); }}><option value="COURSE_FIRST">코스 먼저</option><option value="STAY_FIRST">숙소 먼저</option></select></label>
      {conditions.flowType === 'STAY_FIRST' && <fieldset className="condition-fieldset"><legend>선택한 숙소</legend><p className="conditions-field-note">숙소 선택 기능 연결 전에는 확인된 번호를 입력해 주세요.</p><div className="condition-fields"><label>숙소 번호<input type="number" min="1" step="1" value={conditions.lodgingSelection?.lodgingId ?? ''} onChange={(event) => updateStay('lodgingId', numberOrNull(event.target.value))} /></label><label>객실 번호<input type="number" min="1" step="1" value={conditions.lodgingSelection?.roomId ?? ''} onChange={(event) => updateStay('roomId', numberOrNull(event.target.value))} /></label><label>체크인<input type="date" value={conditions.lodgingSelection?.checkIn ?? ''} onChange={(event) => updateStay('checkIn', event.target.value)} /></label><label>체크아웃<input type="date" value={conditions.lodgingSelection?.checkOut ?? ''} onChange={(event) => updateStay('checkOut', event.target.value)} /></label></div></fieldset>}
      <fieldset className="condition-fieldset"><legend>고정 일정 <span className="optional-label">선택</span></legend>{conditions.fixedEvents.map((item, index) => <div className="fixed-event" key={index}><div className="fixed-event-heading"><strong>일정 {index + 1}</strong><button type="button" onClick={() => update('fixedEvents', conditions.fixedEvents.filter((_, eventIndex) => eventIndex !== index))}>삭제</button></div><div className="condition-fields"><label>날짜<input type="date" value={item.date} onChange={(event) => updateEvent(index, 'date', event.target.value)} /></label><label>시간<input type="time" value={item.startTime} onChange={(event) => updateEvent(index, 'startTime', event.target.value)} /></label></div><label>장소<input maxLength="200" value={item.placeName} onChange={(event) => updateEvent(index, 'placeName', event.target.value)} /></label></div>)}<button className="add-event" type="button" onClick={() => update('fixedEvents', [...conditions.fixedEvents, { date: '', startTime: '', placeName: '' }])}>+ 고정 일정 추가</button></fieldset>
      {errors.length > 0 && <div className="conditions-errors" role="alert"><strong>다음 항목을 확인해 주세요.</strong><ul>{errors.map((error) => <li key={error}>{error}</li>)}</ul></div>}
      <button className="primary condition-confirm" type="submit">입력한 조건 확인</button>
      <p className="conditions-field-note">확인한 조건은 대화에 저장되며, 여행 일정 생성은 아직 연결 전입니다.</p>
    </form>}
  </aside>;
}
