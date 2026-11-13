import { useEffect, useRef, useState } from 'react';
import AccessNotice from '../components/AccessNotice';
import useMemberResource from '../hooks/useMemberResource';
import { settingsApi } from '../api/settingsApi';
import { surveyError } from '../data/settings';
import './survey.css';
import TravelStyleWizard from './TravelStyleWizard';

const QUESTIONS = [
  { key: 'pace', label: '여행 스타일', title: '어떤 여행 스타일이세요?', sub: '나와 가장 가까운 하나를 골라 주세요.', options: [
    ['packed', '알차게 여행', '계획표대로 하루를 꽉 채워요', 'M3 12h4l3-8 4 16 3-8h4'],
    ['normal', '적당한 여행', '계획은 세우되 쉬는 틈도 둬요', 'M12 7v5l3 2 M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18'],
    ['relaxed', '여유로운 여행', '그날 기분 따라 느긋하게', 'M3 18V8M3 14h18v4M21 14v-2a3 3 0 0 0-3-3h-7v5M7 9a2 2 0 1 0 0 4a2 2 0 1 0 0-4'],
  ] },
  { key: 'place', label: '장소 선호', title: '야외와 실내, 어디가 좋으세요?', sub: '하나만 골라 주세요.', options: [
    ['야외', '야외', '자연·산책·전망 좋은 곳', 'M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8'],
    ['실내', '실내', '전시·카페·실내 체험', 'M3 11l9-7 9 7v9H3zM9 20v-6h6v6'],
    ['둘 다', '둘 다', '날씨 따라 골고루', 'M7 7h10l-3-3M17 17H7l3 3'],
  ] },
  { key: 'areas', label: '선호 지역', title: '어떤 풍경 속으로 떠나고 싶으세요?', sub: '마음에 드는 곳을 1~3개 골라 주세요.', options: [
    ['도시', '도시', '맛집·쇼핑·야경이 가까운 곳', 'M3 21h18M5 21V9l5-3v15M10 21V4l9 4v13M13 10h3M13 14h3'],
    ['시골', '시골', '한적한 마을과 들판', 'M3 21h18M5 21v-7l5-4 5 4v7M9 21v-4h2v4M17 21V11h3v10'],
    ['호수·강', '호수·강', '물가 산책과 잔잔한 풍경', 'M2 15c2-1.5 4-1.5 6 0s4 1.5 6 0 4-1.5 6 0M2 19c2-1.5 4-1.5 6 0s4 1.5 6 0 4-1.5 6 0M7 11l5-7 5 7'],
    ['섬', '섬', '배 타고 떠나는 특별한 여행', 'M2 19c2-1.5 4-1.5 6 0s4 1.5 6 0 4-1.5 6 0M6 15c1-3 4-5 8-5s5 2 6 5M11 10V4M11 4c-2 0-4 1-5 3M11 4c2 0 4 1 5 3'],
    ['산·숲', '산·숲', '숲길 걷기와 맑은 공기', 'M2 20l7-12 4 6 3-4 6 10z'],
    ['바다', '바다', '해변과 바닷가 마을', 'M2 12c2-2 4-2 6 0s4 2 6 0 4-2 6 0M2 17c2-2 4-2 6 0s4 2 6 0 4-2 6 0M17 4a2 2 0 1 0 0 4a2 2 0 1 0 0-4'],
  ] },
];

function answersFrom(data) {
  const tags = (data?.tags || []).filter(tag => tag.type === 'style' && tag.polarity === 'like' && tag.weight === 1);
  const hasNewSurvey = tags.some(tag => tag.name?.startsWith('장소:') || tag.name?.startsWith('선호지역:'));
  return { pace: data?.saved && hasNewSurvey && ['packed', 'normal', 'relaxed'].includes(data.pace) ? data.pace : '',
    place: QUESTIONS[1].options.find(([value]) => tags.some(tag => tag.name === `장소:${value}`))?.[0] || '',
    areas: QUESTIONS[2].options.map(([value]) => value).filter(value => tags.some(tag => tag.name === `선호지역:${value}`)).slice(0, 3) };
}

function ChoiceTiles({ question, answers, select, compact, busy }) {
  const multi = question.key === 'areas';
  const selected = answers[question.key];
  return <div className={`st-choice-grid${compact ? ' compact' : ''}`}>
    {question.options.map(([value, name, description, path]) => {
      const pressed = multi ? selected.includes(value) : selected === value;
      return <button key={value} type="button" className="st-choice" aria-pressed={pressed}
        disabled={busy || (multi && selected.length === 3 && !pressed)} onClick={() => select(question.key, value)}>
        <span className="st-choice-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d={path} /></svg></span>
        <span className="st-choice-copy"><strong>{name}</strong>{!compact && <small>{description}</small>}</span>
      </button>;
    })}
  </div>;
}

function Questions({ member, data, surveyMode, onSaved }) {
  const [answers, setAnswers] = useState(() => answersFrom(data));
  const [step, setStep] = useState(0);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const mounted = useRef(true);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => { setAnswers(answersFrom(data)); }, [data]);
  function select(key, value) {
    setNotice('');
    setAnswers(previous => ({ ...previous, [key]: key === 'areas'
      ? previous.areas.includes(value) ? previous.areas.filter(name => name !== value) : previous.areas.length < 3 ? [...previous.areas, value] : previous.areas
      : value }));
  }
  async function save(event) {
    event?.preventDefault();
    if (submitting.current) return;
    const error = surveyError(answers.pace, answers.place, answers.areas);
    if (error) { setNotice(error); return; }
    submitting.current = true; setBusy(true); setNotice('');
    try {
      const tags = [`장소:${answers.place}`, ...answers.areas.map(name => `선호지역:${name}`)]
        .map(name => ({ type: 'style', name, polarity: 'like', weight: 1 }));
      await settingsApi.savePreferences(member.memberId, { pace: answers.pace, wakeUp: data?.wakeUp || 'normal', tags, surveyOnly: true });
      if (mounted.current) { if (!surveyMode) setNotice('저장했어요'); onSaved?.(); }
    } catch (requestError) {
      if (mounted.current) setNotice(requestError.status === 403 ? '여행자만 선호도를 저장할 수 있어요.' : requestError.status === 400 ? '선택한 항목을 확인해 주세요.' : requestError.message);
    } finally { submitting.current = false; if (mounted.current) setBusy(false); }
  }
  if (surveyMode) {
    const question = QUESTIONS[step];
    const ready = question.key === 'areas' ? answers.areas.length >= 1 : Boolean(answers[question.key]);
    return <section className="st-survey" aria-label="여행 선호도 조사" aria-busy={busy}>
      <div className="st-survey-dots" aria-label={`${step + 1} / 3단계`}>{QUESTIONS.map((item, index) => <span key={item.key} className={index <= step ? 'on' : ''} />)}</div>
      <p className="st-survey-step">{step + 1} / 3 · {question.label}</p><h1>{question.title}</h1>
      <p className="st-survey-sub">{question.sub}{question.key === 'areas' && <span> ({answers.areas.length} / 3 고름)</span>}</p>
      <ChoiceTiles question={question} answers={answers} select={select} busy={busy} />
      <div className="st-survey-actions"><button type="button" className="outline" onClick={() => { setStep(step - 1); setNotice(''); }} disabled={busy} style={{ visibility: step ? 'visible' : 'hidden' }}>이전</button>
        <div><button type="button" className="primary" disabled={busy || !ready} onClick={step === 2 ? save : () => { setStep(step + 1); setNotice(''); }}>{step === 2 ? '저장하기' : '다음'}</button></div></div>
      {notice && <p className="st-survey-notice" role="alert">{notice}</p>}
    </section>;
  }
  return <div className="mypage st-style-page"><h1>내 여행 스타일 관리</h1><form onSubmit={save}>
    {QUESTIONS.map(question => <section className="st-style-section" key={question.key}><h2>{question.label}{question.key === 'areas' && <small> (1~3개)</small>}</h2>
      <ChoiceTiles question={question} answers={answers} select={select} compact busy={busy} /></section>)}
    <div className="st-style-actions"><button type="submit" className="primary" disabled={busy || Boolean(surveyError(answers.pace, answers.place, answers.areas))}>저장</button>
      {notice && <span role="status" className={notice === '저장했어요' ? 'saved' : 'error'}>{notice}</span>}</div></form></div>;
}

export default function TravelStylePage({ surveyMode = false, member, initialData, onSaved }) {
  const resource = useMemberResource(surveyMode ? null : member?.memberId, settingsApi.preferences);
  if (surveyMode) return <Questions member={member} data={initialData} surveyMode onSaved={onSaved} />;
  if (resource.status === 'loading') return <p role="status">여행 스타일을 불러오고 있습니다.</p>;
  if (resource.error) return <><AccessNotice kind={resource.error.status === 401 ? 'login' : resource.error.status === 403 ? 'denied' : 'unavailable'} onRetry={resource.reload} /><p className="mypage" role="alert">{resource.error.message}</p></>;
  return <TravelStyleWizard key={member.memberId} member={member} initialData={resource.data} onSaved={onSaved} />;
}
