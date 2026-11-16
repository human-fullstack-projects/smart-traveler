import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

// 휴대폰 본인인증 팝업 (시연용) — 실제 문자 발송·서버 저장 없음. 인증번호는 화면에 보여 주는 DEMO_CODE.
const DEMO_CODE = '123456';
const LIMIT_SECONDS = 180;

function formatPhone(value) {
  const d = value.replace(/\D/g, '').slice(0, 11);
  if (d.length < 4) return d;
  if (d.length < 8) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, d.length - 4)}-${d.slice(-4)}`;
}

export default function IdentityModal({ onVerified, onClose }) {
  const dialogRef = useRef(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [sent, setSent] = useState(false);
  const [left, setLeft] = useState(LIMIT_SECONDS);
  const [code, setCode] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    const opener = document.activeElement;
    document.body.style.overflow = 'hidden';
    dialogRef.current?.querySelector('input')?.focus();
    function onKeyDown(event) {
      if (event.key === 'Escape') { event.preventDefault(); closeRef.current(); return; }
      if (event.key !== 'Tab') return;
      // Tab 키가 팝업 밖(뒤 회원가입 화면)으로 나가지 않게 (약관 팝업과 같은 방식)
      const focusable = [...dialogRef.current.querySelectorAll('button:not(:disabled), input:not(:disabled)')];
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => { document.removeEventListener('keydown', onKeyDown); document.body.style.overflow = previousOverflow; opener?.focus?.(); };
  }, []);

  useEffect(() => {
    if (!sent || left <= 0) return undefined;
    const timer = setTimeout(() => setLeft((s) => s - 1), 1000);
    return () => clearTimeout(timer);
  }, [sent, left]);

  function requestCode() {
    if (!name.trim()) { setError('이름을 입력해주세요.'); return; }
    if (!/^01[016789]-?\d{3,4}-?\d{4}$/.test(phone)) { setError('휴대폰 번호를 확인해주세요. 예: 010-1234-5678'); return; }
    setError(''); setCode(''); setSent(true); setLeft(LIMIT_SECONDS);
  }

  function confirm() {
    if (left <= 0) { setError('입력 시간이 지났어요. 인증번호를 다시 받아주세요.'); return; }
    if (code !== DEMO_CODE) { setError('인증번호가 맞지 않아요.'); return; }
    onVerified({ name: name.trim(), phone });
  }

  const mm = Math.floor(left / 60);
  const ss = String(left % 60).padStart(2, '0');

  return createPortal(
    <div className="auth-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="auth-modal identity-modal" ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby="identity-title">
        <div className="auth-modal-head">
          <h2 id="identity-title">휴대폰 본인인증</h2>
          <p className="st-note">가입하려면 이름과 휴대폰 번호로 본인인증을 해주세요.</p>
        </div>
        <div className="identity-body">
          <label className="st-field"><span>이름 <span className="auth-required" aria-hidden="true">*</span></span>
            <input className="st-input" value={name} placeholder="홍길동" autoComplete="name"
              onChange={(e) => { setName(e.target.value); setError(''); }} /></label>
          <label className="st-field"><span>휴대폰 번호 <span className="auth-required" aria-hidden="true">*</span></span>
            <div className="identity-inline">
              <input className="st-input" type="tel" value={phone} placeholder="010-1234-5678" autoComplete="tel"
                onChange={(e) => { setPhone(formatPhone(e.target.value)); setError(''); }} />
              <button type="button" className="st-btn st-btn-line" onClick={requestCode}>인증번호 받기</button>
            </div></label>
          {sent && <>
            <p className="identity-demo" role="status">문자를 보냈어요 (시연용 인증번호: <b>{DEMO_CODE}</b>)</p>
            <label className="st-field"><span>인증번호 6자리</span>
              <div className="identity-inline">
                <input className="st-input" inputMode="numeric" maxLength={6} value={code} placeholder="000000" autoComplete="one-time-code"
                  onChange={(e) => { setCode(e.target.value.replace(/\D/g, '').slice(0, 6)); setError(''); }}
                  onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); confirm(); } }} />
                <span className={`identity-timer${left <= 30 ? ' low' : ''}`}>{mm}:{ss}</span>
              </div></label>
          </>}
          {error && <p className="st-error" role="alert">{error}</p>}
        </div>
        <div className="auth-modal-actions">
          <button type="button" className="st-btn" onClick={confirm} disabled={!sent || code.length !== 6}>인증 완료</button>
          <button type="button" className="st-btn st-btn-line" onClick={onClose}>취소</button>
        </div>
        <p className="identity-foot">시연용 화면이에요 · 실제 문자 발송과 본인확인은 하지 않아요</p>
      </section>
    </div>, document.body,
  );
}
