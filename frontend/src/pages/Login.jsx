import { useEffect, useRef, useState } from 'react';
import './Login.css';
import { login } from '../api/auth';

export default function Login({ onLogin }) {
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [visible, setVisible] = useState(false);
  const [errors, setErrors] = useState({});
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const inFlight = useRef(false);
  const mounted = useRef(true);
  const titleRef = useRef(null);
  const idRef = useRef(null);
  const passwordRef = useRef(null);

  useEffect(() => { mounted.current = true; titleRef.current?.focus(); return () => { mounted.current = false; }; }, []);

  async function submit(event) {
    event.preventDefault();
    if (inFlight.current) return;
    const nextErrors = {};
    if (!loginId.trim()) nextErrors.loginId = '아이디를 입력해주세요.';
    if (!password) nextErrors.password = '비밀번호를 입력해주세요.';
    setErrors(nextErrors);
    setNotice('');
    if (nextErrors.loginId) { idRef.current?.focus(); return; }
    if (nextErrors.password) { passwordRef.current?.focus(); return; }
    inFlight.current = true;
    setBusy(true);
    try {
      const member = await login(loginId.trim(), password);
      // Update application auth state even if the user navigated away while submitting.
      onLogin(member);
    } catch (error) {
      if (mounted.current) setNotice(error.message);
    } finally {
      inFlight.current = false;
      if (mounted.current) {
        setPassword('');
        setVisible(false);
        setBusy(false);
      }
    }
  }

  function update(field, value) {
    if (field === 'loginId') setLoginId(value);
    else setPassword(value);
    setErrors((previous) => ({ ...previous, [field]: '' }));
    setNotice('');
  }

  return (
    <section className="login-page" aria-labelledby="login-title">
      <a className="login-back" href="#/">← 여행 시작으로 돌아가기</a>
      <div className="login-card">
        <div className="login-intro">
          <span className="login-kicker">WELCOME BACK</span>
          <h1 id="login-title" ref={titleRef} tabIndex={-1}>다시 만나 반가워요.</h1>
          <p>로그인하고 나만의 여행을 이어가세요.</p>
        </div>
        <form className="login-form" onSubmit={submit} noValidate aria-busy={busy}>
          <div className="login-field">
            <label htmlFor="login-id">아이디</label>
            <input ref={idRef} id="login-id" name="username" type="text"
              disabled={busy} autoComplete="username" autoCapitalize="none" spellCheck={false} required
              placeholder="아이디를 입력해주세요" value={loginId}
              onChange={(event) => update('loginId', event.target.value)}
              aria-invalid={Boolean(errors.loginId)} aria-describedby={errors.loginId ? 'login-id-error' : undefined} />
            {errors.loginId && <p id="login-id-error" className="login-error" role="alert">{errors.loginId}</p>}
          </div>
          <div className="login-field">
            <label htmlFor="login-password">비밀번호</label>
            <div className="login-password-row">
              <input ref={passwordRef} id="login-password" name="password"
                disabled={busy} type={visible ? 'text' : 'password'} autoComplete="current-password" required
                placeholder="비밀번호를 입력해주세요" value={password}
                onChange={(event) => update('password', event.target.value)}
                aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? 'login-password-error' : undefined} />
              <button type="button" className="login-password-toggle" disabled={busy}
                aria-label="비밀번호 표시" aria-pressed={visible} aria-controls="login-password"
                onClick={() => setVisible((value) => !value)}>{visible ? '숨기기' : '보기'}</button>
            </div>
            {errors.password && <p id="login-password-error" className="login-error" role="alert">{errors.password}</p>}
          </div>
          <button className="primary login-submit" type="submit" disabled={busy}>{busy ? '로그인 중…' : '로그인'}</button>
          {notice && <p className="login-notice" role="status">{notice}</p>}
        </form>
        <p className="login-development-note">아이디와 비밀번호는 로그인 확인에만 사용됩니다.</p>
      </div>
    </section>
  );
}
