import { useEffect, useRef, useState } from 'react';
import TossCheckoutPage from '../../payment/TossCheckoutPage.jsx';
import { createReservation } from '../shared/stayApi.js';
import { formatWon } from '../shared/stayData.js';

export default function StayReservationDialog({ open, room, member, checkIn, checkOut, guests, courseId,
  waitingForCourse, onClose, onAvailabilityChanged }) {
  const dialog = useRef(null);
  const submitting = useRef(false);
  const [visitorName, setVisitorName] = useState('');
  const [sameAsBooker, setSameAsBooker] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [reservationId, setReservationId] = useState(null);
  const bookerName = member?.nickname?.trim() || member?.loginId || '';
  const bookerEmail = member?.email?.trim() || '';

  useEffect(() => {
    const element = dialog.current;
    if (open) element.showModal();
    return () => element.close();
  }, [open]);

  function close() {
    if (!submitting.current) onClose();
  }

  async function submit(event) {
    event.preventDefault();
    const visitor = sameAsBooker ? null : visitorName.trim();
    if (!member || !bookerEmail || waitingForCourse || (!sameAsBooker && !visitor) || submitting.current) return;
    const form = new FormData(event.currentTarget);
    submitting.current = true;
    setSaving(true);
    setError('');
    try {
      const reservation = await createReservation({
        roomId: room.roomId, checkIn, checkOut, guestCount: guests,
        visitorName: visitor,
        requestMemo: form.get('requestMemo'),
        agreedToPolicy: form.has('agreedToPolicy'),
        expectedTotalPrice: room.totalPrice,
        ...(courseId ? { courseId } : {}),
      });
      setReservationId(reservation.reservationId);
      if (dialog.current) dialog.current.scrollTop = 0;
    } catch (reason) {
      if (reason.status === 409) onAvailabilityChanged();
      else setError(reason.message || '예약을 진행하지 못했습니다. 다시 시도해 주세요.');
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  }

  return <dialog ref={dialog} className="booking-reservation-dialog" aria-labelledby="booking-reservation-title"
    onCancel={event => { event.preventDefault(); close(); }}>
    <header className="booking-reservation-dialog__header">
      <h2 id="booking-reservation-title">예약 정보</h2>
      <button type="button" onClick={close} disabled={saving} aria-label="예약 창 닫기">닫기</button>
    </header>
    <div className="booking-reservation-dialog__body">
      <p className="booking-reservation-dialog__room"><strong>{room.name}</strong><br />{checkIn} ~ {checkOut} · {guests}명</p>
      {reservationId ? <TossCheckoutPage reservationId={reservationId} embedded visible={open} /> : <>
        <div className="booking-price-lines">
          {room.nights.map(night => <div key={night.date}><span>{night.date}</span><strong>{formatWon(night.price)}</strong></div>)}
          <div className="is-total"><span>총 결제 금액</span><strong>{formatWon(room.totalPrice)}</strong></div>
        </div>
        {member ? <form onSubmit={submit}>
          <label className="booking-form-field">예약자 이름<input value={bookerName} disabled /></label>
          <label className="booking-form-field">연락처 (이메일)<input type="email" value={bookerEmail} disabled /></label>
          {!bookerEmail && <p className="booking-contact-note">예약하려면 <a href="#/my-profile">내 정보</a>에 이메일을 등록해 주세요.</p>}
          <label className="booking-agreement booking-same-visitor">
            <input type="checkbox" checked={sameAsBooker} disabled={saving} onChange={event => {
              setSameAsBooker(event.target.checked);
              setVisitorName('');
            }} />예약자와 동일
          </label>
          <label className="booking-form-field">방문자 이름<input name="visitorName" maxLength={50} autoComplete="off"
            required={!sameAsBooker} value={sameAsBooker ? bookerName : visitorName}
            disabled={sameAsBooker || saving} onChange={event => setVisitorName(event.target.value)} /></label>
          <label className="booking-form-field">요청사항<textarea name="requestMemo" rows="3" maxLength="500" disabled={saving} /></label>
          <label className="booking-agreement"><input name="agreedToPolicy" type="checkbox" required disabled={saving} />예약자와 방문자 정보가 숙소에 전달되며, 10분 이내에 테스트 결제를 진행해야 함을 확인했습니다.</label>
          {waitingForCourse && <p role="status">여행 일정을 불러온 뒤 예약할 수 있습니다.</p>}
          {error && <p className="booking-notice" role="alert">{error}</p>}
          <button className="booking-pay-button" disabled={!bookerEmail || saving || waitingForCourse} type="submit">
            {saving ? '예약 중…' : '예약하고 결제하기'}
          </button>
        </form> : <a className="booking-pay-button is-link" href="#/login">로그인하고 예약하기</a>}
      </>}
    </div>
  </dialog>;
}
