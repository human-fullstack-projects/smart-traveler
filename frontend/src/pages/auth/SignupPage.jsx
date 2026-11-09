import { useEffect, useState } from 'react';
import '../../css/Auth.css';
import { getTerms, signup } from '../../api/auth';
import TermsModal from './TermsModal';

// 회원가입 (AUTH-01 여행자 · AUTH-02 숙박업주 · AUTH-03 약관 동의)
const TERM_NAMES = { SERVICE: '서비스 이용약관', PRIVACY: '개인정보 수집·이용', LOCATION: '위치정보 이용', MARKETING: '마케팅 정보 수신' };
const EMPTY = { loginId: '', password: '', passwordCheck: '', nickname: '', email: '',
  businessNumber: '', businessName: '', representativeName: '', businessAddress: '', businessPhone: '' };

function check(role, f) {
  const e = {};
  if (!/^[A-Za-z0-9_]{4,50}$/.test(f.loginId)) e.loginId = '영문, 숫자, 밑줄 4~50자로 입력해주세요.';
  if (f.password.length < 8 || !/[A-Za-z]/.test(f.password) || !/[0-9]/.test(f.password)) e.password = '영문과 숫자를 포함해 8자 이상 입력해주세요.';
  if (f.password !== f.passwordCheck) e.passwordCheck = '비밀번호가 같지 않아요.';
  if (!f.nickname.trim()) e.nickname = '닉네임을 입력해주세요.';
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(f.email)) e.email = '올바른 이메일을 입력해주세요.';
  if (role === 'OWNER') {
    ['businessNumber', 'businessName', 'representativeName', 'businessAddress', 'businessPhone']
      .forEach((k) => { if (!f[k].trim()) e[k] = '필수 항목이에요.'; });
  }
  return e;
}

function Field({ name, label, form, errors, onChange, type = 'text', placeholder, autoComplete }) {
  return <label className={`st-field${errors[name] ? ' st-field-error' : ''}`}>{label}
    <input className="st-input" name={name} type={type} value={form[name]} placeholder={placeholder}
      autoComplete={autoComplete} onChange={(e) => onChange(name, e.target.value)} aria-invalid={Boolean(errors[name])} />
    {errors[name] && <p className="st-error" role="alert">{errors[name]}</p>}
  </label>;
}

export default function SignupPage() {
  const [role, setRole] = useState('USER');
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [terms, setTerms] = useState([]);
  const [termsState, setTermsState] = useState('loading');
  const [openTerm, setOpenTerm] = useState(null);
  const [returnFocus, setReturnFocus] = useState(null);
  const [agreed, setAgreed] = useState({});
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(null);

  useEffect(() => { getTerms().then((list) => { setTerms(list || []); setTermsState((list || []).length ? 'ok' : 'empty'); }).catch(() => setTermsState('error')); }, []);

  const onChange = (name, value) => { setForm((f) => ({ ...f, [name]: value })); setErrors((e) => ({ ...e, [name]: '' })); setNotice(''); };
  const allAgreed = terms.length > 0 && terms.every((t) => agreed[t.termsId]);
  const toggleAll = () => { const v = !allAgreed; setAgreed(Object.fromEntries(terms.map((t) => [t.termsId, v]))); };

  async function submit(event) {
    event.preventDefault();
    const e = check(role, form);
    setErrors(e);
    if (Object.keys(e).length) { setNotice('입력한 내용을 확인해주세요.'); return; }
    if (terms.some((t) => t.required && !agreed[t.termsId])) { setNotice('필수 약관에 동의해주세요.'); return; }
    setBusy(true); setNotice('');
    try {
      const body = { role, loginId: form.loginId, password: form.password, nickname: form.nickname.trim(), email: form.email.trim(),
        consents: terms.map((t) => ({ termsId: t.termsId, agreed: Boolean(agreed[t.termsId]) })) };
      if (role === 'OWNER') ['businessNumber', 'businessName', 'representativeName', 'businessAddress', 'businessPhone'].forEach((k) => { body[k] = form[k].trim(); });
      setDone(await signup(body));
    } catch (err) {
      setNotice(err.message);
    } finally { setBusy(false); }
  }

  if (done) {
    return <div className="st-page auth-page"><section className="auth-wrap"><div className="st-panel auth-done">
      <p className="auth-kicker">WELCOME</p>
      <h1 className="st-h2">가입이 완료됐어요</h1>
      <p className="st-note">{done.role === 'OWNER' ? '숙박업주 가입은 관리자 승인 후 숙소를 등록할 수 있어요. 승인 결과는 로그인 후 확인해주세요.' : '이제 로그인하고 여행을 시작해보세요.'}</p>
      <p className="st-note">이 사이트에서 보관한 임시 코스는 다음 로그인 시 연결됩니다. 연결이 끝날 때까지 새로고침하거나 페이지를 닫지 마세요.</p>
      <a className="st-btn st-btn-lg" href="#/login">로그인하러 가기</a>
    </div></section></div>;
  }

  const p = { form, errors, onChange };
  return (
    <div className="st-page auth-page">
      <section className="auth-wrap wide" aria-labelledby="signup-title">
        <a className="auth-back" href="#/login">← 로그인으로</a>
        <div className="st-panel">
          <div className="auth-head">
            <p className="auth-kicker">JOIN SMART TRAVEL</p>
            <h1 id="signup-title" className="st-h2">회원가입</h1>
            <p>가입 유형을 고르고 정보를 입력해주세요.</p>
          </div>
          <form className="auth-form" onSubmit={submit} noValidate aria-busy={busy}>
            <div className="auth-roles" role="group" aria-label="가입 유형">
              <button type="button" className="auth-role" aria-pressed={role === 'USER'} onClick={() => setRole('USER')}>
                <strong>여행자</strong><span>AI 코스 추천과 숙소 예약</span></button>
              <button type="button" className="auth-role" aria-pressed={role === 'OWNER'} onClick={() => setRole('OWNER')}>
                <strong>숙박업주</strong><span>내 숙소를 여행자에게 알리기 · 관리자 승인 필요</span></button>
            </div>
            <p className="auth-section-title">기본 정보</p>
            <div className="auth-row">
              <Field {...p} name="loginId" label="아이디" placeholder="영문·숫자 4자 이상" autoComplete="username" />
              <Field {...p} name="nickname" label="닉네임" placeholder="화면에 보일 이름" autoComplete="nickname" />
            </div>
            <div className="auth-row">
              <Field {...p} name="password" label="비밀번호" type="password" placeholder="영문+숫자 8자 이상" autoComplete="new-password" />
              <Field {...p} name="passwordCheck" label="비밀번호 확인" type="password" placeholder="한 번 더 입력" autoComplete="new-password" />
            </div>
            <Field {...p} name="email" label="이메일" type="email" placeholder="example@email.com" autoComplete="email" />
            {role === 'OWNER' && <>
              <p className="auth-section-title">사업자 정보</p>
              <div className="auth-row">
                <Field {...p} name="businessName" label="상호명" placeholder="예: 경주 한옥스테이" />
                <Field {...p} name="businessNumber" label="사업자등록번호" placeholder="000-00-00000" />
              </div>
              <div className="auth-row">
                <Field {...p} name="representativeName" label="대표자 이름" />
                <Field {...p} name="businessPhone" label="사업장 연락처" type="tel" placeholder="054-000-0000" />
              </div>
              <Field {...p} name="businessAddress" label="사업장 주소" placeholder="도로명 주소" />
            </>}
            <p className="auth-section-title">약관 동의</p>
            <div className="auth-terms">
              {terms.length === 0 ? <span className="st-note">{termsState === 'loading' ? '약관을 불러오는 중이에요.' : termsState === 'error' ? '약관을 불러오지 못했어요. 서버가 켜져 있는지 확인한 뒤 새로고침해 주세요.' : '등록된 약관이 없어요. 관리자에게 문의해 주세요.'}</span> : <>
                <label className="all"><input type="checkbox" checked={allAgreed} onChange={toggleAll} />전체 동의</label>
                {terms.map((t) => <div key={t.termsId} className="auth-term">
                  <div className="auth-term-row">
                    <label>
                      <input type="checkbox" checked={Boolean(agreed[t.termsId])} onChange={() => setAgreed((a) => ({ ...a, [t.termsId]: !a[t.termsId] }))} />
                      {TERM_NAMES[t.type] || t.type} {t.required ? <span className="auth-req">필수</span> : <span className="st-note">선택</span>}
                    </label>
                    <button type="button" className="auth-term-view" onClick={(event) => { setReturnFocus(event.currentTarget); setOpenTerm(t); }}>보기</button>
                  </div>
                </div>)}
              </>}
            </div>
            <button className="st-btn st-btn-lg auth-submit" type="submit" disabled={busy}>{busy ? '가입 중…' : '가입하기'}</button>
            {notice && <p className="auth-notice error" role="status">{notice}</p>}
          </form>
          <p className="auth-foot">이미 회원이신가요? <a href="#/login">로그인</a></p>
        </div>
      </section>
      {openTerm && <TermsModal term={openTerm} title={TERM_NAMES[openTerm.type] || openTerm.type}
        returnFocus={returnFocus} onClose={() => setOpenTerm(null)}
        onAgree={() => { setAgreed((current) => ({ ...current, [openTerm.termsId]: true })); setOpenTerm(null); }} />}
    </div>
  );
}
