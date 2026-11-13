import { useEffect, useRef, useState } from 'react';
import AccessNotice from '../components/AccessNotice';
import useMemberResource from '../hooks/useMemberResource';
import { settingsApi } from '../api/settingsApi';
import { SURVEY_REGIONS, SURVEY_MOODS, surveyError } from '../data/settings';
import '../mypage-settings.css';
import './survey.css';
import TravelStyleWizard from './TravelStyleWizard';

export default function TravelStylePage({ surveyMode = false, ...props }) {
  return surveyMode ? <TravelStyleSurvey {...props} /> : <NormalTravelStylePage {...props} />;
}

function TravelStyleSurvey({ member, initialData, onSkip, onSaved }) {
  const catalog = Array.isArray(initialData?.catalog) ? initialData.catalog : [
    ...SURVEY_REGIONS.map(name => ({ type: 'style', name: `지역:${name}` })),
    ...SURVEY_MOODS.map(name => ({ type: 'style', name: `분위기:${name}` })),
  ];
  const regions = SURVEY_REGIONS.filter(name => catalog.some(tag => tag.type === 'style' && tag.name === `지역:${name}`));
  const moods = SURVEY_MOODS.filter(name => catalog.some(tag => tag.type === 'style' && tag.name === `분위기:${name}`));
  const [selected, setSelected] = useState(() => SURVEY_REGIONS.filter(name => initialData?.tags?.some(tag => tag.type === 'style' && tag.name === `지역:${name}` && tag.polarity === 'like' && tag.weight === 1)).slice(0, 3));
  const [mood, setMood] = useState(() => SURVEY_MOODS.find(name => initialData?.tags?.some(tag => tag.type === 'style' && tag.name === `분위기:${name}` && tag.polarity === 'like' && tag.weight === 1)) || '');
  const [step, setStep] = useState(1);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const mounted = useRef(true);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  function toggleRegion(name) {
    setNotice('');
    if (selected.includes(name)) setSelected(previous => previous.filter(value => value !== name));
    else if (selected.length === 3) setNotice('3곳까지 고를 수 있어요');
    else setSelected(previous => [...previous, name]);
  }
  async function save() {
    if (submitting.current) return;
    const error = surveyError(selected, mood);
    if (error) { setNotice(error); return; }
    submitting.current = true; setBusy(true); setNotice('');
    try {
      const tags = [...selected.map(name => ({ type: 'style', name: `지역:${name}`, polarity: 'like', weight: 1 })),
        { type: 'style', name: `분위기:${mood}`, polarity: 'like', weight: 1 }];
      await settingsApi.savePreferences(member.memberId, { pace: 'normal', wakeUp: 'normal', tags, surveyOnly: true });
      if (mounted.current) onSaved();
    } catch (requestError) {
      if (mounted.current) setNotice(requestError.status === 403 ? '여행자만 선호도 조사를 저장할 수 있어요.' : requestError.status === 400 ? '선택한 항목을 확인해 주세요.' : requestError.message);
    } finally { submitting.current = false; if (mounted.current) setBusy(false); }
  }
  return <div className="st-survey"><div className="st-survey-intro"><p>MY TRAVEL STYLE</p><h1>{step === 1 ? '가고 싶은 지역을 3개 골라주세요!' : '어떤 여행이 좋으세요?'}</h1><p>여행 취향을 알려주시면 나에게 어울리는 여행을 찾기 쉬워져요.</p></div>
    <section className="st-survey-panel" aria-label="여행 선호도 조사" aria-busy={busy}>
      <div className="st-survey-progress" aria-label={`${step} / 2단계`}><span className={`dot${step === 1 ? ' active' : ''}`} />지역 선택<span className="line" /><span className={`dot${step === 2 ? ' active' : ''}`} />여행 분위기</div>
      {step === 1 ? <><div className="st-survey-heading"><h2>가고 싶은 지역</h2><span aria-live="polite">{selected.length} / 3</span></div><p className="st-survey-help">가장 마음이 가는 곳 세 군데를 골라주세요.</p>
        <div className="st-survey-regions" aria-label="가고 싶은 지역">{regions.map(name => <button key={name} type="button" aria-pressed={selected.includes(name)} onClick={() => toggleRegion(name)} disabled={busy}>{name}</button>)}</div>
        <div className="st-survey-actions"><button type="button" className="secondary" onClick={onSkip}>나중에 하기</button><button type="button" className="primary" disabled={selected.length !== 3} onClick={() => { setStep(2); setNotice(''); }}>다음</button></div></>
        : <><div className="st-survey-summary"><strong>고른 지역 {selected.length} / 3</strong>{selected.join(' · ')}</div><div className="st-survey-heading"><h2>여행 분위기</h2></div><p className="st-survey-help">평소 좋아하는 여행 분위기를 하나 골라주세요.</p>
          <div className="st-survey-moods">{moods.map(name => <button key={name} type="button" aria-pressed={mood === name} onClick={() => { setMood(name); setNotice(''); }} disabled={busy}><span className="check" aria-hidden="true">✓</span><span><strong>{name}이 좋아요</strong><small>{name === '조용한 여행' ? '등산, 사람 적은 관광명소 등' : '서울처럼 사람 많은 지역'}</small></span></button>)}</div>
          <div className="st-survey-actions"><button type="button" className="secondary" onClick={() => { setStep(1); setNotice(''); }} disabled={busy}>이전</button><button type="button" className="secondary" onClick={onSkip} disabled={busy}>나중에 하기</button><button type="button" className="primary" disabled={busy || Boolean(surveyError(selected, mood))} onClick={save}>{busy ? '저장 중…' : '저장하고 시작하기'}</button></div></>}
      {notice && <p className="st-survey-notice" role="status">{notice}</p>}
    </section></div>;
}

function NormalTravelStylePage({ member }) {
  const resource = useMemberResource(member.memberId, settingsApi.preferences);
  if (resource.status === 'loading') return <p role="status">여행 스타일을 불러오고 있습니다.</p>;
  if (resource.error) return <><AccessNotice kind={resource.error.status === 401 ? 'login' : resource.error.status === 403 ? 'denied' : 'unavailable'} onRetry={resource.reload} /><p className="mypage" role="alert">{resource.error.message}</p></>;
  return <TravelStyleWizard key={member.memberId} member={member} initialData={resource.data} />;
}
