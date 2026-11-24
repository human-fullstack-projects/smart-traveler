import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../../../contexts/AuthContext';
import Card from '../components/Card';
import PageHero from '../components/PageHero';
import ProfileAvatar from '../components/ProfileAvatar';
import AccessNotice from '../components/AccessNotice';
import useMemberResource from '../hooks/useMemberResource';
import { settingsApi } from '../api/settingsApi';
import { profileError } from '../data/settings';
import { prepareAvatar } from './avatar';
import { avatarPresets } from './avatarPresets';
import PasswordChangeDialog from './PasswordChangeDialog';
import '../mypage-settings.css';

const EMPTY = { nickname: '', email: '', avatarData: null };
export default function ProfilePage({ member }) {
  const resource = useMemberResource(member.memberId, settingsApi.profile);
  const { updateMemberProfile } = useAuth();
  const [form, setForm] = useState(EMPTY);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [reading, setReading] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const mounted = useRef(true);
  const submitting = useRef(false);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
  useEffect(() => { if (resource.data) setForm({ ...EMPTY, nickname: resource.data.nickname || '', email: resource.data.email || '', avatarData: resource.data.avatarData }); }, [resource.data]);
  const update = (name, value) => { setForm(previous => ({ ...previous, [name]: value })); setNotice(''); };

  async function photo(event) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setReading(true); setNotice('');
    try { const avatar = await prepareAvatar(file); if (mounted.current) { update('avatarData', avatar); setNotice('사진을 선택했습니다. 저장 버튼을 눌러 반영해 주세요.'); } }
    catch (error) { if (mounted.current) setNotice(error.message || '이미지를 읽지 못했습니다.'); }
    finally { if (mounted.current) setReading(false); }
  }
  async function selectPreset(preset) {
    if (busy || reading) return;
    setReading(true); setNotice('');
    try {
      const response = await fetch(preset.src);
      if (!response.ok) throw new Error('이미지를 불러오지 못했습니다.');
      const blob = await response.blob();
      const avatar = await prepareAvatar(new File([blob], `${preset.id}.png`, { type: blob.type }));
      if (mounted.current) {
        update('avatarData', avatar);
        setNotice(`${preset.label} 이미지를 선택했습니다. 내 정보 저장 버튼을 눌러 반영해 주세요.`);
      }
    } catch (error) { if (mounted.current) setNotice(error.message || '이미지를 불러오지 못했습니다.'); }
    finally { if (mounted.current) setReading(false); }
  }
  async function submit(event) {
    event.preventDefault();
    if (submitting.current || reading) return;
    const error = profileError(form);
    if (error) { setNotice(error); return; }
    submitting.current = true; setBusy(true); setNotice('');
    try {
      const result = await settingsApi.saveProfile(member.memberId, { ...form, nickname: form.nickname.trim(), email: form.email.trim(), changePassword: false });
      updateMemberProfile(member.memberId, {
        nickname: result.profile.nickname,
        email: result.profile.email,
        avatarData: result.profile.avatarData,
      });
      if (mounted.current) { resource.accept(result.profile); setNotice(result.passwordChanged ? '내 정보와 비밀번호를 변경했습니다. 다음 로그인부터 새 비밀번호를 사용해 주세요.' : '내 정보를 저장했습니다.'); }
    } catch (error) { if (mounted.current) setNotice(error.message); }
    finally { submitting.current = false; if (mounted.current) setBusy(false); }
  }
  if (resource.status === 'loading') return <p role="status">내 정보를 불러오고 있습니다.</p>;
  if (resource.error) return <><AccessNotice kind={resource.error.status === 401 ? 'login' : resource.error.status === 403 ? 'denied' : 'unavailable'} onRetry={resource.reload} /><p className="mypage" role="alert">{resource.error.message}</p></>;
  return <div className="mypage mp-settings mp-profile-settings"><PageHero eyebrow="MY PROFILE" title="내 정보관리" />
    <Card><form onSubmit={submit}><fieldset disabled={busy || reading}>
      <div className="mp-profile-line"><label className="mp-profile-image-label" htmlFor="mp-profile-image-upload"><span>프로필 이미지</span></label><ProfileAvatar avatarData={form.avatarData} name={form.nickname} /><div><input id="mp-profile-image-upload" type="file" accept="image/png,image/jpeg,image/webp" onChange={photo} /><p className="note">1MB 이하 · 저장 시 최대 256픽셀로 조정합니다.</p>
        <div className="mp-avatar-options" role="group" aria-label="프로필 이미지 선택">
          <button className="mp-avatar-option mp-avatar-option-default" type="button" aria-label="기본 프로필 이미지 선택" aria-pressed={!form.avatarData} title="기본 이미지" onClick={() => { update('avatarData', null); setNotice('기본 이미지를 선택했습니다. 내 정보 저장 버튼을 눌러 반영해 주세요.'); }}><ProfileAvatar /></button>
          {avatarPresets.map(preset => <button key={preset.id} className="mp-avatar-option" type="button" onClick={() => selectPreset(preset)} aria-label={`${preset.label} 프로필 이미지 선택`} title={preset.label}><img src={preset.src} alt="" width="52" height="52" /></button>)}
        </div>
      </div></div>
      <div className="mp-profile-fields-grid">
        <div><label>아이디<input value={resource.data.loginId} readOnly disabled tabIndex={-1} aria-readonly="true" autoComplete="username" /></label><p className="note">아이디는 변경할 수 없습니다.</p></div>
        <label>닉네임<input name="nickname" value={form.nickname} maxLength={50} onChange={e => update('nickname', e.target.value)} required autoComplete="nickname" /></label>
        <div className="mp-password-action"><span>비밀번호</span><button type="button" className="secondary" onClick={() => setPasswordOpen(true)}>비밀번호 변경</button></div>
        <label>이메일<input name="email" type="email" value={form.email} maxLength={255} onChange={e => update('email', e.target.value)} required autoComplete="email" /></label>
      </div>
      <div className="actions mp-profile-actions">
        {notice && <p className="mp-profile-notice" role="status">{notice}</p>}
        <button className="primary" type="submit" aria-busy={busy || reading}>내 정보 저장</button>
        <a className="secondary" href="#/mypage">마이트립페이지</a>
      </div>
    </fieldset>
    </form></Card>
    {passwordOpen && <PasswordChangeDialog key={member.memberId} memberId={member.memberId} onClose={() => setPasswordOpen(false)} onChanged={() => setNotice('비밀번호를 변경했습니다. 다음 로그인부터 새 비밀번호를 사용해 주세요.')} />}
  </div>;
}
