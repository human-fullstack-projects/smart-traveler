import { useEffect, useState } from 'react';
import { getOwnerReview, reapplyOwner } from '../../api/auth';
import '../../css/Auth.css';

const label = { PENDING: '승인 대기', APPROVED: '승인', REJECTED: '반려' };

export default function OwnerReviewPage({ onContinue }) {
  const [review, setReview] = useState(null);
  const [form, setForm] = useState(null);
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    getOwnerReview().then((data) => { setReview(data); setForm(data); })
      .catch((error) => setNotice(error.message));
  }, []);

  async function submit(event) {
    event.preventDefault();
    setBusy(true); setNotice('');
    try {
      const data = await reapplyOwner(form);
      setReview(data); setForm(data); setNotice('재신청을 받았어요. 심사 결과를 기다려주세요.');
    } catch (error) { setNotice(error.message); }
    finally { setBusy(false); }
  }

  return <div className="st-page auth-page owner-auth-page"><section className="auth-wrap wide st-panel" aria-labelledby="review-title">
    <p className="auth-kicker">OWNER REVIEW</p>
    <h1 id="review-title" className="st-h2">업주 신청 상태</h1>
    {review && <>
      <p className="auth-notice">현재 상태: <strong>{label[review.status] || review.status}</strong></p>
      {review.status === 'REJECTED' && <p className="auth-notice error">반려 사유: {review.history.find((h) => h.status === 'REJECTED')?.comment || '등록된 사유가 없습니다.'}</p>}
      {review.status === 'REJECTED' && <form className="auth-form" onSubmit={submit}>
        <p className="auth-section-title">사업 정보를 수정한 뒤 다시 신청해주세요.</p>
        {[[ 'businessNumber', '사업자번호' ], [ 'businessName', '상호명' ], [ 'representativeName', '대표자명' ], [ 'businessAddress', '사업장 주소' ], [ 'businessPhone', '연락처' ]].map(([key, text]) =>
          <label key={key} className="st-field">{text}<input className="st-input" value={form?.[key] || ''} required={key !== 'businessAddress' && key !== 'businessPhone'} disabled={busy}
            onChange={(e) => setForm((old) => ({ ...old, [key]: e.target.value }))} /></label>)}
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
