import { useState } from 'react';
import { settingsApi } from '../api/settingsApi';
import './travel-style-wizard.css';

const steps = [
  { label: '여행 스타일', title: '어떤 여행 스타일이세요?', help: '나와 가장 가까운 하나를 골라 주세요.', options: [
    ['packed', '알차게 여행', '계획표대로 하루를 꽉 채워요', 'pulse', '파워 J에게 추천'],
    ['normal', '적당한 여행', '계획은 세우되 쉬는 틈도 둬요', 'clock', 'J와 P가 섞인 분께 추천'],
    ['relaxed', '여유로운 여행', '그날 기분 따라 느긋하게', 'bed', 'P에게 추천'],
  ] },
  { label: '장소 선호', title: '야외와 실내, 어디가 좋으세요?', help: '하나만 골라 주세요.', options: [
    ['outdoor', '야외', '자연·산책·전망 좋은 곳', 'sun'],
    ['indoor', '실내', '전시·카페·실내 체험', 'home'],
    ['both', '둘 다', '날씨 따라 골고루', 'arrows'],
  ] },
  { label: '선호 지역', title: '어떤 풍경 속으로 떠나고 싶으세요?', help: '마음에 드는 곳을 1~3개 골라 주세요.', options: [
    ['도시', '도시', '맛집·쇼핑·야경이 가까운 곳', 'city'],
    ['농촌', '시골', '한적한 마을과 들판', 'village'],
    ['호수·강', '호수·강', '물가 산책과 잔잔한 풍경', 'lake'],
    ['섬', '섬', '배 타고 떠나는 특별한 여행', 'island'],
    ['산·숲', '산·숲', '숲길 걷기와 맑은 공기', 'mountain'],
    ['바다·해변', '바다', '해변과 바닷가 마을', 'sea'],
  ] },
];
const paths = {
  pulse: 'M2 12h5l3-8 4 16 3-8h5', clock: 'M12 7v5l4 2 M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0',
  bed: 'M3 6v14 M3 15h18v5 M7 11h14v4 M7 11V8h4v3 M21 15V9h-7v6',
  sun: 'M12 2v2 M12 20v2 M2 12h2 M20 12h2 M5 5l2 2 M17 17l2 2 M5 19l2-2 M17 7l2-2 M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  home: 'M3 10l9-7 9 7v11H3z M9 21v-8h6v8', arrows: 'M6 7h12l-3-3 M18 17H6l3 3',
  city: 'M3 21h18 M5 21V7l6-3v17 M11 7l8 3v11 M8 9v2 M8 14v2 M14 12h2 M14 16h2',
  village: 'M2 12l7-6 7 6 M4 11v10h11V11 M8 21v-6h4v6 M18 21V7h3v14',
  lake: 'M6 11l6-8 6 8 M2 16q3-4 6 0t6 0t6 0 M2 21q3-4 6 0t6 0t6 0',
  island: 'M12 17V6 M5 8q7-10 14 0 M12 6l-5 6 M12 6l6 6 M2 21q4-5 8-1t12 0',
  mountain: 'M2 20l7-13 5 8 4-7 5 12z M7 11l2 3 2-2', sea: 'M2 14q3-4 6 0t6 0t6 0 M2 20q3-4 6 0t6 0t6 0 M19 5a2 2 0 1 1-4 0 2 2 0 0 1 4 0',
};

export default function TravelStyleWizard({ member, initialData, onSaved }) {
  const tags = initialData?.tags || [];
  const liked = name => tags.some(tag => tag.type === 'style' && tag.name === name && tag.polarity === 'like');
  const placeLiked = name => liked(`장소:${name}`) || liked(name);
  const [pace, setPace] = useState(initialData?.saved ? initialData.pace : '');
  const [place, setPlace] = useState(placeLiked('둘 다') || (placeLiked('실내') && placeLiked('야외')) ? 'both' : placeLiked('실내') ? 'indoor' : placeLiked('야외') ? 'outdoor' : '');
  const areaName = name => name === '농촌' ? '시골' : name === '바다·해변' ? '바다' : name;
  const [regions, setRegions] = useState(steps[2].options.filter(([name]) => liked(`선호지역:${areaName(name)}`) || liked(name)).map(([name]) => name).slice(0, 3));
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const valid = Boolean(pace) && Boolean(place) && regions.length > 0;
  function choose(step, value) {
    setNotice('');
    if (step === 0) setPace(value);
    else if (step === 1) setPlace(value);
    else setRegions(previous => previous.includes(value) ? previous.filter(item => item !== value) : previous.length < 3 ? [...previous, value] : previous);
  }
  async function save() {
    if (busy || !valid) return;
    setBusy(true); setNotice('');
    const names = [`장소:${place === 'both' ? '둘 다' : place === 'indoor' ? '실내' : '야외'}`, ...regions.map(name => `선호지역:${areaName(name)}`)];
    try {
      await settingsApi.savePreferences(member.memberId, {
        pace, wakeUp: initialData?.wakeUp || 'normal',
        tags: names.map(name => ({ type: 'style', name, polarity: 'like', weight: 1 })), surveyOnly: true,
      });
      setNotice('여행 스타일을 저장했습니다.');
      onSaved?.();
    } catch (error) { setNotice(error.message || '저장하지 못했습니다. 다시 시도해 주세요.'); }
    finally { setBusy(false); }
  }
  return <div className="travel-style-page">
    <h1 className="travel-style-page-title">내 여행 스타일 관리</h1>
    <section className="travel-style-panel" aria-label="여행 선호도 3질문" aria-busy={busy}>
      <div className="travel-style-inner">
        {steps.map((current, step) => <section className="travel-style-section" key={current.label} aria-labelledby={`travel-style-heading-${step}`}>
        <p className="travel-style-step">{step + 1} / 3 · {current.label}</p>
        <h2 id={`travel-style-heading-${step}`}>{current.title}</h2>
        <p className="travel-style-help">{current.help} {step === 2 && <strong aria-live="polite">({regions.length} / 3 고름)</strong>}</p>
        <div className="travel-style-options" role="group" aria-label={current.label}>
          {current.options.map(([value, title, description, icon, badge]) => {
            const selected = step === 0 ? pace === value : step === 1 ? place === value : regions.includes(value);
            return <button type="button" key={value} className="travel-style-option" aria-pressed={selected} disabled={busy || (step === 2 && regions.length === 3 && !selected)} onClick={() => choose(step, value)}>
              <span className="travel-style-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d={paths[icon]} /></svg></span>
              {selected && <span className="travel-style-check" aria-hidden="true">✓</span>}
              <strong>{title}</strong>{badge && <span className="travel-style-badge">{badge}</span>}<span className="travel-style-description">{description}</span>
            </button>;
          })}
        </div>
        </section>)}
        <div className="travel-style-actions">
          <div className="travel-style-actions-right">
            <button type="button" className="travel-style-next" disabled={busy || !valid} onClick={save}>{busy ? '저장 중…' : '저장하기'}</button>
          </div>
        </div>
        {notice && <p className="travel-style-notice" role="status">{notice}</p>}
      </div>
    </section>
  </div>;
}
