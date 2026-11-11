import { useEffect, useRef, useState } from 'react';
import '../../css/Auth.css';
import { login } from '../../api/auth';
import { getSocialProviders } from './socialApi';
import OwnerReviewPage from './OwnerReviewPage';
import { useAuth } from '../../contexts/AuthContext';

// 로그인 (AUTH) — 처리 로직은 sein/v1 그대로, 화면만 공유 CSS(st-*)로.
export default function LoginPage({ onLogin, guestCourseCount = 0 }) {
  const { member: currentMember, login: updateAuth } = useAuth();
  const [loginId, setLoginId] = useState('');
  const [password, setPassword] = useState('');
  const [visible, setVisible] = useState(false);
  const [errors, setErrors] = useState({});
  const [notice, setNotice] = useState(new URLSearchParams(window.location.search).get('socialError') || '');
  const [providers, setProviders] = useState({ google: false, kakao: false });
  const [busy, setBusy] = useState(false);
  const [reviewMember, setReviewMember] = useState(null);
  const inFlight = useRef(false);
  const mounted = useRef(true);
  const titleRef = useRef(null);
  const idRef = useRef(null);
  const passwordRef = useRef(null);

  useEffect(() => { mounted.current = true; titleRef.current?.focus(); return () => { mounted.current = false; }; }, []);
  useEffect(() => { getSocialProviders().then(setProviders).catch(() => {}); }, []);

  function socialLogin(provider) {
    const base = import.meta.env.VITE_SOCIAL_AUTH_BASE_URL
      || (import.meta.env.DEV ? `${window.location.protocol}//${window.location.hostname}:8080` : window.location.origin);
    window.location.assign(`${base}/oauth2/authorization/${provider}`);
  }

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
      if (member.roleCode === 'OWNER') { updateAuth(member); setReviewMember(member); }
      else onLogin(member);
    } catch (error) {
      if (mounted.current) setNotice(error.message);
    } finally {
      inFlight.current = false;
      if (mounted.current) { setPassword(''); setVisible(false); setBusy(false); }
    }
  }

  function update(field, value) {
    if (field === 'loginId') setLoginId(value); else setPassword(value);
    setErrors((previous) => ({ ...previous, [field]: '' }));
    setNotice('');
  }

  if (reviewMember && currentMember?.roleCode === 'OWNER') return <OwnerReviewPage onContinue={() => onLogin(reviewMember)} />;
  return (
    <div className="st-page auth-page">
      <section className="auth-wrap" aria-labelledby="login-title">
        <a className="auth-back" href="#/">← 여행 시작으로</a>
        <div className="st-panel">
          <div className="auth-head">
            <p className="auth-kicker">WELCOME BACK</p>
            <h1 id="login-title" className="st-h2" ref={titleRef} tabIndex={-1}>다시 만나 반가워요</h1>
            <p>로그인하고 나만의 여행을 이어가세요.</p>
            {guestCourseCount > 0 && <p className="st-note">보관한 여행 코스 {guestCourseCount}건을 로그인한 계정의 내 여행 코스로 연결합니다. 연결이 끝날 때까지 새로고침하거나 사이트를 떠나지 마세요.</p>}
          </div>
          <form className="auth-form" onSubmit={submit} noValidate aria-busy={busy}>
            <label className={`st-field${errors.loginId ? ' st-field-error' : ''}`}>아이디
              <input ref={idRef} className="st-input" name="username" type="text" disabled={busy}
                autoComplete="username" autoCapitalize="none" spellCheck={false}
                placeholder="아이디를 입력해주세요" value={loginId}
                onChange={(e) => update('loginId', e.target.value)} aria-invalid={Boolean(errors.loginId)} />
              {errors.loginId && <p className="st-error" role="alert">{errors.loginId}</p>}
            </label>
            <div className="auth-password">
              <label className={`st-field${errors.password ? ' st-field-error' : ''}`}>비밀번호
                <input ref={passwordRef} className="st-input" name="password" disabled={busy}
                  type={visible ? 'text' : 'password'} autoComplete="current-password"
                  placeholder="비밀번호를 입력해주세요" value={password}
                  onChange={(e) => update('password', e.target.value)} aria-invalid={Boolean(errors.password)} />
              </label>
              <button type="button" className="st-btn st-btn-line" disabled={busy} aria-pressed={visible}
                onClick={() => setVisible((v) => !v)}>{visible ? '숨기기' : '보기'}</button>
            </div>
            {errors.password && <p className="st-error" role="alert">{errors.password}</p>}
            <button className="st-btn st-btn-lg auth-submit" type="submit" disabled={busy}>{busy ? '로그인 중…' : '로그인'}</button>
            {notice && <p className="auth-notice error" role="status">{notice}</p>}
          </form>
          <div className="auth-social" aria-label="소셜 로그인">
            <button type="button" className="st-btn st-btn-line" disabled={!providers.google}
              onClick={() => socialLogin('google')}>구글로 로그인{!providers.google && ' · 준비 중'}</button>
            <button type="button" className="st-btn st-btn-line" disabled={!providers.kakao}
              onClick={() => socialLogin('kakao')}>카카오로 로그인{!providers.kakao && ' · 준비 중'}</button>
          </div>
          <p className="auth-foot">아직 회원이 아니신가요? <a href="#/signup">회원가입</a></p>
        </div>
      </section>
    </div>
  );
}
