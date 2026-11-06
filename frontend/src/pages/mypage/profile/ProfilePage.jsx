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
import '../mypage-settings.css';

const EMPTY = { nickname: '', email: '', avatarData: null, changePassword: false, currentPassword: '', newPassword: '', passwordConfirmation: '' };
export default function ProfilePage({ member }) {
  const resource = useMemberResource(member.memberId, settingsApi.profile);
  const { updateMemberProfile } = useAuth();
  const [form, setForm] = useState(EMPTY);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [reading, setReading] = useState(false);
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
  async function submit(event) {
    event.preventDefault();
    if (submitting.current || reading) return;
    const error = profileError(form);
    if (error) { setNotice(error); return; }
    submitting.current = true; setBusy(true); setNotice('');
    try {
      const result = await settingsApi.saveProfile(member.memberId, { ...form, nickname: form.nickname.trim(), email: form.email.trim(),
        currentPassword: form.changePassword ? form.currentPassword : null,
        newPassword: form.changePassword ? form.newPassword : null, passwordConfirmation: form.changePassword ? form.passwordConfirmation : null });
      updateMemberProfile(member.memberId, { nickname: result.profile.nickname });
      if (mounted.current) { resource.accept(result.profile); setNotice(result.passwordChanged ? '내 정보와 비밀번호를 변경했습니다. 다음 로그인부터 새 비밀번호를 사용해 주세요.' : '내 정보를 저장했습니다.'); }
    } catch (error) { if (mounted.current) setNotice(error.message); }
    finally { submitting.current = false; if (mounted.current) { setBusy(false); setForm(previous => ({ ...previous, currentPassword: '', newPassword: '', passwordConfirmation: '' })); } }
  }
  if (resource.status === 'loading') return <p role="status">내 정보를 불러오고 있습니다.</p>;
  if (resource.error) return <><AccessNotice kind={resource.error.status === 401 ? 'login' : resource.error.status === 403 ? 'denied' : 'unavailable'} onRetry={resource.reload} /><p className="mypage" role="alert">{resource.error.message}</p></>;
  return <div className="mypage mp-settings"><PageHero eyebrow="MY PROFILE" title="내 정보관리">로그인 계정의 기본 정보를 관리하세요.</PageHero>
    <Card><form onSubmit={submit}><fieldset disabled={busy || reading}>
      <div className="mp-profile-line"><ProfileAvatar avatarData={form.avatarData} name={form.nickname} /><div><label>프로필 이미지<input type="file" accept="image/png,image/jpeg,image/webp" onChange={photo} /></label><p className="note">1MB 이하 · 저장 시 최대 256픽셀로 조정합니다.</p><button className="secondary" type="button" onClick={() => update('avatarData', null)}>기본 이미지 사용</button></div></div>
      <label>아이디<input value={resource.data.loginId} readOnly aria-readonly="true" autoComplete="username" /></label><p className="note">아이디는 변경할 수 없습니다.</p>
      <label>닉네임<input name="nickname" value={form.nickname} maxLength={50} onChange={e => update('nickname', e.target.value)} required autoComplete="nickname" /></label>
      <label>이메일<input name="email" type="email" value={form.email} maxLength={255} onChange={e => update('email', e.target.value)} required autoComplete="email" /></label>
      <label className="checkline"><input type="checkbox" checked={form.changePassword} onChange={e => setForm(previous => ({ ...previous, changePassword: e.target.checked, currentPassword: '', newPassword: '', passwordConfirmation: '' }))} />비밀번호 변경</label>
      {form.changePassword && <div className="mp-password-fields"><label>현재 비밀번호<input type="password" value={form.currentPassword} autoComplete="current-password" onChange={e => update('currentPassword', e.target.value)} required /></label><label>새 비밀번호<input type="password" value={form.newPassword} autoComplete="new-password" onChange={e => update('newPassword', e.target.value)} required /></label><label>새 비밀번호 확인<input type="password" value={form.passwordConfirmation} autoComplete="new-password" onChange={e => update('passwordConfirmation', e.target.value)} required /></label><p className="note">영문·숫자 포함 8자 이상, UTF-8 기준 72바이트 이하</p></div>}
      <div className="actions"><button className="primary" type="submit">{busy ? '저장 중…' : reading ? '사진 처리 중…' : '내 정보 저장'}</button><a className="secondary" href="#/mypage">마이페이지로</a></div>
    </fieldset></form>{notice && <p role="status">{notice}</p>}</Card>
  </div>;
}
