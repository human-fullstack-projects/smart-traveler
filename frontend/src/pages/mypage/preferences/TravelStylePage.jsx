import { useEffect, useRef, useState } from 'react';
import Card from '../components/Card';
import PageHero from '../components/PageHero';
import AccessNotice from '../components/AccessNotice';
import useMemberResource from '../hooks/useMemberResource';
import { settingsApi } from '../api/settingsApi';
import { PACE_OPTIONS, WAKE_OPTIONS, TAG_GROUPS, tagKey, preferenceError, preferenceNotices } from '../data/settings';
import '../mypage-settings.css';

export default function TravelStylePage({ member }) {
  const resource = useMemberResource(member.memberId, settingsApi.preferences);
  const [form, setForm] = useState({ pace: 'normal', wakeUp: 'normal', tags: [] });
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const mounted = useRef(true);
  const submitting = useRef(false);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => { if (resource.data) setForm({ pace: resource.data.pace, wakeUp: resource.data.wakeUp, tags: resource.data.tags }); }, [resource.data]);
  function toggle(option, checked) {
    setNotice('');
    setForm(previous => ({ ...previous, tags: checked ? [...previous.tags, { ...option, polarity: 'like', weight: 1 }] : previous.tags.filter(t => tagKey(t) !== tagKey(option)) }));
  }
  function edit(option, field, value) {
    setNotice('');
    setForm(previous => ({ ...previous, tags: previous.tags.map(t => tagKey(t) === tagKey(option) ? { ...t, [field]: value } : t) }));
  }
  async function submit(event) {
    event.preventDefault();
    if (submitting.current) return;
    const error = preferenceError(form, resource.data.catalog);
    if (error) { setNotice(error); return; }
    submitting.current = true; setBusy(true); setNotice('');
    try {
      const result = await settingsApi.savePreferences(member.memberId, form);
      if (mounted.current) { resource.accept(result); setNotice('여행 스타일을 저장했습니다. 다음 여행을 계획할 때 참고할 기본 취향입니다.'); }
    } catch (error) { if (mounted.current) setNotice(error.message); }
    finally { submitting.current = false; if (mounted.current) setBusy(false); }
  }
  if (resource.status === 'loading') return <p role="status">여행 스타일을 불러오고 있습니다.</p>;
  if (resource.error) return <><AccessNotice kind={resource.error.status === 401 ? 'login' : resource.error.status === 403 ? 'denied' : 'unavailable'} onRetry={resource.reload} /><p className="mypage" role="alert">{resource.error.message}</p></>;
  return <div className="mypage mp-settings"><PageHero eyebrow="MY TRAVEL STYLE" title="내 여행 스타일 관리">나에게 편안한 여행의 기준을 정해 보세요.</PageHero>
    <div className="mp-settings-notices">{preferenceNotices.map(text => <p key={text}>{text}</p>)}</div>
    <form onSubmit={submit}><fieldset disabled={busy}>
      <Card><p className="note">{resource.data.saved ? '저장된 기본 취향입니다. 원하는 항목을 수정해 주세요.' : '아직 저장된 취향이 없습니다. 기본값에서 시작할 수 있습니다.'}</p>
        <fieldset className="mp-choice-group"><legend>일정 강도</legend>{PACE_OPTIONS.map(([code, label]) => <label key={code}><input type="radio" name="pace" value={code} checked={form.pace === code} onChange={() => { setForm(previous => ({ ...previous, pace: code })); setNotice(''); }} />{label}</label>)}</fieldset>
        <fieldset className="mp-choice-group"><legend>기상 시간대</legend>{WAKE_OPTIONS.map(([code, label]) => <label key={code}><input type="radio" name="wakeUp" value={code} checked={form.wakeUp === code} onChange={() => { setForm(previous => ({ ...previous, wakeUp: code })); setNotice(''); }} />{label}</label>)}</fieldset>
      </Card>
      {TAG_GROUPS.map(([type, label]) => <Card key={type} style={{ marginTop: 20 }}><h2>{label} 태그</h2><p className="note">선택한 태그마다 선호·기피와 강도를 지정하세요. 1은 약하게, 3은 강하게 반영할 기본값입니다.</p>
        <div className="mp-tag-grid">{resource.data.catalog.filter(t => t.type === type).map(option => {
          const selected = form.tags.find(t => tagKey(t) === tagKey(option));
          return <div className={`mp-tag-row${selected ? ' is-selected' : ''}`} key={tagKey(option)}>
            <label className="checkline"><input type="checkbox" checked={Boolean(selected)} onChange={e => toggle(option, e.target.checked)} />{option.name}</label>
            {selected && <div className="mp-tag-controls"><label>선호 여부<select aria-label={`${option.name} 선호 여부`} value={selected.polarity} onChange={e => edit(option, 'polarity', e.target.value)}><option value="like">선호</option><option value="avoid">기피</option></select></label><label>강도<select aria-label={`${option.name} 강도`} value={selected.weight} onChange={e => edit(option, 'weight', Number(e.target.value))}>{[1, 2, 3].map(n => <option key={n} value={n}>{n}</option>)}</select></label></div>}
          </div>;
        })}</div>
      </Card>)}
      <div className="actions"><button className="primary" type="submit">{busy ? '저장 중…' : '여행 스타일 저장'}</button><a className="secondary" href="#/mypage">마이페이지로</a></div>
    </fieldset></form>{notice && <p role="status" className="mp-save-notice">{notice}</p>}
  </div>;
}
