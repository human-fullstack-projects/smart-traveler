import { useEffect, useState } from 'react';
import { useAuth } from '../../../contexts/AuthContext.jsx';
import { createReservation, getRooms, getStay } from '../shared/stayApi.js';
import { StayFooter, StayHeader } from '../shared/StayShell.jsx';
import { formatWon, getDefaultStayDates, getNightCount } from '../shared/stayData.js';
import '../../../css/stay-booking.css';

export default function StayBookingPage() {
  const query = new URLSearchParams(window.location.hash.split('?')[1]);
  const lodgingId = query.get('stay');
  const defaults = getDefaultStayDates();
  const { member } = useAuth();
  const [checkIn, setCheckIn] = useState(query.get('checkIn') || defaults.checkIn);
  const [checkOut, setCheckOut] = useState(query.get('checkOut') || defaults.checkOut);
  const [guests, setGuests] = useState(Number(query.get('adults') || 2) + Number(query.get('children') || 0));
  const [stay, setStay] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [roomId, setRoomId] = useState(null);
  const [notice, setNotice] = useState('');
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [saving, setSaving] = useState(false);
  const [refresh, setRefresh] = useState(0);
  const room = rooms.find((item) => item.roomId === roomId);
  const nights = getNightCount(checkIn, checkOut);

  useEffect(() => {
    if (!lodgingId) return;
    let active = true;
    getStay(lodgingId)
      .then((data) => { if (active) setStay(data); })
      .catch((error) => { if (active) setNotice(error.status === 404 ? '숙소를 찾을 수 없습니다.' : error.message); });
    return () => { active = false; };
  }, [lodgingId]);

  useEffect(() => {
    if (!lodgingId || !nights || !guests) {
      setLoadingRooms(false);
      return;
    }
    let active = true;
    setLoadingRooms(true);
    getRooms(lodgingId, checkIn, checkOut, guests)
      .then((data) => { if (active) setRooms(data); })
      .catch((error) => { if (active) setNotice(error.message); })
      .finally(() => { if (active) setLoadingRooms(false); });
    return () => { active = false; };
  }, [lodgingId, checkIn, checkOut, guests, nights, refresh]);

  function changeCondition(setter, value) {
    setRoomId(null);
    setRooms([]);
    setNotice('');
    setter(value);
  }

  async function submit(event) {
    event.preventDefault();
    if (!room || !member || saving) return;
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setNotice('');
    try {
      const reservation = await createReservation({
        roomId: room.roomId, checkIn, checkOut, guestCount: guests,
        bookerName: form.get('bookerName'), bookerPhone: form.get('bookerPhone'),
        requestMemo: form.get('requestMemo'), agreedToPolicy: form.has('agreedToPolicy'),
        expectedTotalPrice: room.totalPrice,
      });
      window.location.hash = '/payments/' + reservation.reservationId;
    } catch (error) {
      setNotice(error.status === 409 ? '객실 재고 또는 요금이 변경되었습니다. 다시 선택해 주세요.' : error.message);
      if (error.status === 409) {
        setRoomId(null);
        setRooms([]);
        setRefresh((value) => value + 1);
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="stay-ui stay-booking-page">
      <StayHeader active="booking" />
      <main className="booking-main">
        <p><a href="#/stays">← 숙소 검색</a></p>
        {!lodgingId && <p>숙소 검색에서 예약할 숙소를 선택해 주세요.</p>}
        {notice && <p className="booking-notice" role="alert">{notice}</p>}
        {stay && <>
          <div className="booking-title-row">
            <div><h1>{stay.name}</h1><p>{stay.address}</p>
              <p>체크인 {stay.checkinTime?.slice(0, 5)} · 체크아웃 {stay.checkoutTime?.slice(0, 5)}</p></div>
          </div>
          <div className="booking-layout">
            <section className="booking-section">
              <h2>객실 선택</h2>
              <div className="booking-date-grid">
                <label>체크인<input type="date" value={checkIn} disabled={saving} onChange={(event) => changeCondition(setCheckIn, event.target.value)} /></label>
                <label>체크아웃<input type="date" value={checkOut} min={checkIn} disabled={saving} onChange={(event) => changeCondition(setCheckOut, event.target.value)} /></label>
              </div>
              <label className="booking-form-field">인원<input type="number" min="1" max="18" value={guests} disabled={saving} onChange={(event) => changeCondition(setGuests, Number(event.target.value))} /></label>
              {loadingRooms && <p role="status">객실을 조회하고 있습니다.</p>}
              {!loadingRooms && !nights && <p>체크인보다 늦은 체크아웃 날짜를 선택해 주세요.</p>}
              {!loadingRooms && nights > 0 && !notice && rooms.length === 0 && <p>선택한 조건에 예약할 수 있는 객실이 없습니다.</p>}
              <div className="booking-room-list">
                {rooms.map((item) => (
                  <article key={item.roomId} className={'booking-room' + (roomId === item.roomId ? ' is-selected' : '')}>
                    <div className="booking-room__body">
                      <h3>{item.name}</h3>
                      <p>최대 {item.maxCapacity}명 · 잔여 {item.remaining}실</p>
                      <div className="booking-room__bottom">
                        <strong>{nights}박 {formatWon(item.totalPrice)}</strong>
                        <button type="button" disabled={saving} onClick={() => { setRoomId(item.roomId); setNotice(''); }}>
                          {roomId === item.roomId ? '선택됨' : '객실 선택'}
                        </button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
            <aside className="booking-sidebar">
              <form onSubmit={submit}>
                <h2>예약 정보</h2>
                {room && <div className="booking-price-lines">
                  {room.nights.map((night) => <div key={night.date}><span>{night.date}</span><strong>{formatWon(night.price)}</strong></div>)}
                  <div className="is-total"><span>총 결제 금액</span><strong>{formatWon(room.totalPrice)}</strong></div>
                </div>}
                <label className="booking-form-field">예약자 이름<input name="bookerName" maxLength="50" required /></label>
                <label className="booking-form-field">연락처<input name="bookerPhone" type="tel" maxLength="30" required /></label>
                <label className="booking-form-field">요청사항<textarea name="requestMemo" rows="3" maxLength="500" /></label>
                <label className="booking-agreement"><input name="agreedToPolicy" type="checkbox" required />예약자 정보가 숙소에 전달되며, 10분 이내에 테스트 결제를 진행해야 함을 확인했습니다.</label>
                {member
                  ? <button className="booking-pay-button" disabled={!room || saving} type="submit">{saving ? '예약 중…' : '예약하고 결제하기'}</button>
                  : <a className="booking-pay-button is-link" href="#/login">로그인하고 예약하기</a>}
              </form>
            </aside>
          </div>
        </>}
      </main>
      <StayFooter />
    </div>
  );
}
