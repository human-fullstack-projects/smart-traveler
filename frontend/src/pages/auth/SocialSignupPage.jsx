import { useEffect, useState } from 'react';
import '../../css/Auth.css';
import { getTerms } from '../../api/auth';
import { getPendingSocial, socialSignup } from './socialApi';
import TermsModal from './TermsModal';

const TERM_NAMES = { SERVICE: '서비스 이용약관', PRIVACY: '개인정보 수집·이용', LOCATION: '위치정보 이용', MARKETING: '마케팅 정보 수신' };

export default function SocialSignupPage({ onLogin }) {
  const [profile, setProfile] = useState(null);
  const [nickname, setNickname] = useState('');
  const [terms, setTerms] = useState([]);
  const [agreed, setAgreed] = useState({});
  const [openTerm, setOpenTerm] = useState(null);
  const [returnFocus, setReturnFocus] = useState(null);
  const [notice, setNotice] = useState('');
  const [nicknameError, setNicknameError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    Promise.all([getPendingSocial(), getTerms()]).then(([pending, active]) => {
      setProfile(pending); setNickname(pending.nickname || ''); setTerms(active || []);
    }).catch((error) => setNotice(error.message));
  }, []);

  async function submit(event) {
    event.preventDefault();
    if (!nickname.trim() || nickname.trim().length > 50) { setNicknameError('닉네임은 1~50자로 입력해주세요.'); return; }
    setNicknameError('');
    if (!terms.length) { setNotice('약관을 불러오지 못했어요.'); return; }
    if (terms.some((term) => term.required && !agreed[term.termsId])) { setNotice('필수 약관에 동의해주세요.'); return; }
    setBusy(true); setNotice('');
    try {
      const member = await socialSignup({ nickname: nickname.trim(),
        consents: terms.map((term) => ({ termsId: term.termsId, agreed: Boolean(agreed[term.termsId]) })) });
      onLogin(member);
    } catch (error) { setNotice(error.message); }
    finally { setBusy(false); }
  }

  return <div className="st-page auth-page"><section className="auth-wrap" aria-labelledby="social-signup-title">
    <a className="auth-back" href="#/login">← 로그인으로</a>
    <div className="st-panel">
      <div className="auth-head"><p className="auth-kicker">SOCIAL JOIN</p>
        <h1 id="social-signup-title" className="st-h2">여행자 가입</h1>
        <p>{profile ? `${profile.provider === 'GOOGLE' ? '구글' : '카카오'} 계정으로 가입합니다.` : '소셜 로그인 정보를 확인하고 있어요.'}</p></div>
      {profile && <form className="auth-form" onSubmit={submit} aria-busy={busy}>
        <p className="auth-required-legend"><span className="auth-required" aria-hidden="true">*</span> 표시는 꼭 입력해야 해요</p>
        <label className={`st-field${nicknameError ? ' st-field-error' : ''}`}><span>닉네임 <span className="auth-required" aria-hidden="true">*</span></span><input className="st-input" value={nickname}
          aria-required="true" aria-describedby="social-nickname-hint" aria-invalid={Boolean(nicknameError)}
          onChange={(event) => { setNickname(event.target.value); setNicknameError(''); setNotice(''); }} />
          <span id="social-nickname-hint" className="auth-hint">공백만 입력할 수 없으며, 1~50자로 입력해주세요.</span>
          {nicknameError && <p className="st-error" role="alert">{nicknameError}</p>}</label>
        {profile.email && <p className="st-note">이메일: {profile.email}</p>}
        <p className="auth-section-title">약관 동의</p>
        <div className="auth-terms">
          <label className="all"><input type="checkbox" checked={terms.length > 0 && terms.every((term) => agreed[term.termsId])}
            onChange={(event) => setAgreed(Object.fromEntries(terms.map((term) => [term.termsId, event.target.checked])))} />전체 동의</label>
          {terms.map((term) => <div key={term.termsId} className="auth-term-row"><label>
            <input type="checkbox" checked={Boolean(agreed[term.termsId])} aria-required={term.required ? 'true' : undefined}
              onChange={() => setAgreed((current) => ({ ...current, [term.termsId]: !current[term.termsId] }))} />
            {TERM_NAMES[term.type] || term.type} {term.required && <span className="auth-required" aria-hidden="true">*</span>} <span className={term.required ? 'auth-req' : 'st-note'}>{term.required ? '필수' : '선택'}</span>
          </label><button type="button" className="auth-term-view"
            onClick={(event) => { setReturnFocus(event.currentTarget); setOpenTerm(term); }}>보기</button></div>)}
        </div>
        <button type="submit" className="st-btn st-btn-lg auth-submit" disabled={busy}>{busy ? '가입 중…' : '가입하고 시작하기'}</button>
      </form>}
      {notice && <p className="auth-notice error" role="status">{notice}</p>}
    </div></section>
    {openTerm && <TermsModal term={openTerm} title={TERM_NAMES[openTerm.type] || openTerm.type}
      returnFocus={returnFocus} onClose={() => setOpenTerm(null)}
      onAgree={() => { setAgreed((current) => ({ ...current, [openTerm.termsId]: true })); setOpenTerm(null); }} />}
  </div>;
}
