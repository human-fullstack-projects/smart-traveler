import { useEffect, useState } from 'react';
import { getOwnerReview, reapplyOwner } from '../../api/auth';
import '../../css/Auth.css';

const label = { PENDING: '승인 대기', APPROVED: '승인', REJECTED: '반려' };
const OWNER_FIELDS = [
  ['businessNumber', '사업자번호', 20, true],
  ['businessName', '상호명', 100, true],
  ['representativeName', '대표자명', 50, true],
  ['businessAddress', '사업장 주소', 255, false],
  ['businessPhone', '연락처', 20, false],
];

export default function OwnerReviewPage({ onContinue }) {
  const [review, setReview] = useState(null);
  const [form, setForm] = useState(null);
  const [notice, setNotice] = useState('');
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    getOwnerReview().then((data) => { setReview(data); setForm(data); })
      .catch((error) => setNotice(error.message));
  }, []);

  async function submit(event) {
    event.preventDefault();
    const nextErrors = {};
    OWNER_FIELDS.forEach(([key, , max, required]) => {
      const value = (form?.[key] || '').trim();
      if (required && !value) nextErrors[key] = '필수 항목이에요.';
      else if (value.length > max) nextErrors[key] = `${max}자 이내로 입력해주세요.`;
    });
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) { setNotice('입력한 내용을 확인해주세요.'); return; }
    setBusy(true); setNotice('');
    try {
      const data = await reapplyOwner(form);
      setReview(data); setForm(data); setNotice('재신청을 받았어요. 심사 결과를 기다려주세요.');
    } catch (error) { setNotice(error.message); }
    finally { setBusy(false); }
  }

  return <div className="st-page auth-page owner-auth-page"><section className="auth-wrap wide" aria-labelledby="review-title">
    <p className="auth-kicker">OWNER REVIEW</p>
    <h1 id="review-title" className="st-h2">업주 신청 상태</h1>
    {review && <>
      <p className="auth-notice">현재 상태: <strong>{label[review.status] || review.status}</strong></p>
      {review.status === 'REJECTED' && <p className="auth-notice error">반려 사유: {review.history.find((h) => h.status === 'REJECTED')?.comment || '등록된 사유가 없습니다.'}</p>}
      {review.status === 'REJECTED' && <form className="auth-form" onSubmit={submit} noValidate>
        <p className="auth-required-legend"><span className="auth-required" aria-hidden="true">*</span> 표시는 꼭 입력해야 해요</p>
        <p className="auth-section-title">사업 정보를 수정한 뒤 다시 신청해주세요.</p>
        {OWNER_FIELDS.map(([key, text, max, required]) =>
          <label key={key} className={`st-field${errors[key] ? ' st-field-error' : ''}`}><span>{text} {required && <span className="auth-required" aria-hidden="true">*</span>}</span>
            <input className="st-input" value={form?.[key] || ''} aria-required={required ? 'true' : undefined}
              aria-describedby={`review-${key}-hint`} aria-invalid={Boolean(errors[key])} disabled={busy}
              onChange={(e) => { setForm((old) => ({ ...old, [key]: e.target.value })); setErrors((old) => ({ ...old, [key]: '' })); setNotice(''); }} />
            <span id={`review-${key}-hint`} className="auth-hint">{required ? '공백만 입력할 수 없으며, ' : ''}{max}자 이내로 입력해주세요.</span>
            {errors[key] && <p className="st-error" role="alert">{errors[key]}</p>}
          </label>)}
        <button className="st-btn" disabled={busy} type="submit">{busy ? '제출 중…' : '재신청'}</button>
      </form>}
      <h2 className="auth-section-title">심사 이력</h2>
      <ul className="auth-review-history">{review.history.map((h, index) => <li key={index}>
        <strong>{label[h.status] || h.status}</strong> · {String(h.reviewedAt || h.createdAt || '').replace('T', ' ').slice(0, 16)}
        {h.comment && <span> · {h.comment}</span>}
      </li>)}</ul>
    </>}
    {notice && <p className="auth-notice" role="status">{notice}</p>}
    <button className="st-btn st-btn-line" type="button" onClick={onContinue}>여행 화면으로 계속</button>
  </section></div>;
}
