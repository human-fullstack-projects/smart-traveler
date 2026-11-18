import { useEffect, useRef, useState } from 'react';
import { settingsApi } from '../api/settingsApi';
import { passwordError } from '../data/settings';

const EMPTY = { currentPassword: '', newPassword: '', passwordConfirmation: '' };

export default function PasswordChangeDialog({ memberId, onClose, onChanged }) {
  const dialog = useRef(null);
  const mounted = useRef(false);
  const submitting = useRef(false);
  const [form, setForm] = useState(EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    mounted.current = true;
    const element = dialog.current;
    element.showModal();
    return () => { mounted.current = false; element.close(); };
  }, []);
  function update(name, value) { setForm(previous => ({ ...previous, [name]: value })); setError(''); }
  async function submit(event) {
    event.preventDefault();
    if (submitting.current) return;
    const message = passwordError(form);
    if (message) { setError(message); return; }
    submitting.current = true; setBusy(true); setError('');
    try {
      await settingsApi.changePassword(memberId, form);
      if (mounted.current) { onChanged(); onClose(); }
    } catch (failure) {
      if (mounted.current) setError(failure.message || '비밀번호를 변경하지 못했습니다.');
    } finally {
      submitting.current = false;
      if (mounted.current) setBusy(false);
    }
  }
  return <dialog ref={dialog} className="mp-password-dialog" aria-labelledby="mp-password-dialog-title"
    onCancel={event => { event.preventDefault(); if (!busy) onClose(); }}>
    <h2 id="mp-password-dialog-title">비밀번호 변경</h2>
    <form onSubmit={submit}>
      <fieldset disabled={busy}>
        <label>현재 비밀번호<input type="password" name="currentPassword" autoComplete="current-password" autoFocus required value={form.currentPassword} onChange={event => update('currentPassword', event.target.value)} /></label>
        <label>변경할 비밀번호<input type="password" name="newPassword" autoComplete="new-password" required value={form.newPassword} onChange={event => update('newPassword', event.target.value)} /></label>
        <label>변경할 비밀번호 재확인<input type="password" name="passwordConfirmation" autoComplete="new-password" required value={form.passwordConfirmation} onChange={event => update('passwordConfirmation', event.target.value)} /></label>
        <p className="note">영문·숫자 포함 8자 이상</p>
        {error && <p className="mp-password-error" role="alert">{error}</p>}
        <div className="actions"><button type="button" className="secondary" onClick={onClose}>취소</button><button type="submit" className="primary">{busy ? '변경 중…' : '변경'}</button></div>
      </fieldset>
    </form>
  </dialog>;
}
