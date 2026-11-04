import { useEffect, useMemo, useState } from 'react';
import { StayArtwork, StayFooter, StayHeader, StatusPill } from '../shared/StayShell.jsx';
import { formatWon, getDefaultStayDates, getNightCount, stays } from '../shared/stayData.js';
import './stay-booking.css';

function readBookingQuery() {
  const query = new URLSearchParams((window.location.hash.split('?')[1] || '').trim());
  const defaults = getDefaultStayDates();
  return {
    stayId: query.get('stay') || stays.find((stay) => stay.saleable)?.id,
    checkIn: query.get('checkIn') || defaults.checkIn,
    checkOut: query.get('checkOut') || defaults.checkOut,
    adults: Number(query.get('adults')) || 2,
    children: Number(query.get('children')) || 0,
  };
}

function PriceSummary({ room, nights }) {
  if (!room) return <div className="booking-price-empty">객실을 선택하면 최종 금액을 계산합니다.</div>;
  const roomTotal = room.price * nights;
  const serviceFee = 0;
  return (
    <div className="booking-price-lines">
      <div><span>{formatWon(room.price)} × {nights}박</span><strong>{formatWon(roomTotal)}</strong></div>
      <div><span>서비스 수수료</span><strong>{formatWon(serviceFee)}</strong></div>
      <div className="is-total"><span>총 결제 예정 금액</span><strong>{formatWon(roomTotal + serviceFee)}</strong></div>
    </div>
  );
}

export default function StayBookingPage() {
  const initial = useMemo(() => readBookingQuery(), []);
  const [stayId, setStayId] = useState(initial.stayId);
  const [dates, setDates] = useState({ checkIn: initial.checkIn, checkOut: initial.checkOut });
  const [guests, setGuests] = useState({ adults: initial.adults, children: initial.children });
  const [roomId, setRoomId] = useState('');
  const [courseMode, setCourseMode] = useState('before');
  const [booker, setBooker] = useState({ name: '', phone: '', request: '' });
  const [agreed, setAgreed] = useState(false);
  const [notice, setNotice] = useState('');
  const [orderState, setOrderState] = useState('editing');
  const [secondsLeft, setSecondsLeft] = useState(600);
  const stay = stays.find((item) => item.id === stayId && item.saleable) || stays.find((item) => item.saleable);
  const room = stay.rooms.find((item) => item.id === roomId);
  const nights = getNightCount(dates.checkIn, dates.checkOut);
  const guestCount = guests.adults + guests.children;

  useEffect(() => {
    setRoomId('');
    setOrderState('editing');
  }, [stayId]);

  useEffect(() => {
    if (orderState !== 'held') return undefined;
    if (secondsLeft <= 0) {
      setOrderState('expired');
      setNotice('임시 점유 시간이 끝났습니다. 객실을 다시 확인해 주세요.');
      return undefined;
    }
    const timer = window.setInterval(() => setSecondsLeft((current) => current - 1), 1000);
    return () => window.clearInterval(timer);
  }, [orderState, secondsLeft]);

  const timerText = `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`;

  function createOrder(event) {
    event.preventDefault();
    if (!nights) return setNotice('체크인보다 늦은 체크아웃 날짜를 선택해 주세요.');
    if (!room) return setNotice('예약할 객실을 선택해 주세요.');
    if (guestCount > room.maxGuests) return setNotice('선택한 객실의 최대 인원을 초과했습니다.');
    if (!booker.name.trim() || !booker.phone.trim()) return setNotice('예약자 이름과 연락처를 입력해 주세요.');
    if (!agreed) return setNotice('취소·환불 규정과 개인정보 제공 내용을 확인해 주세요.');
    setSecondsLeft(600);
    setOrderState('held');
    setNotice('객실을 10분 동안 임시 점유했습니다. 아래 예약 내용을 확인해 주세요.');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function finishDemoPayment() {
    setOrderState('confirmed');
    setNotice('프론트엔드 데모 예약이 완료되었습니다. 실제 결제 승인과 예약 확정은 서버 연동 후 처리됩니다.');
  }

  return (
    <div className="stay-ui stay-booking-page">
      <StayHeader active="booking" />
      <main className="booking-main">
        <div className="booking-breadcrumb"><a href="#/stays">숙소 검색</a><span>›</span><strong>{stay.name}</strong></div>
        {orderState !== 'editing' && (
          <section className={`booking-order-banner booking-order-banner--${orderState}`} role="status">
            <div><span>{orderState === 'confirmed' ? '예약확정 UI 데모' : orderState === 'expired' ? '임시 점유 만료' : '결제대기 · 객실 임시 점유'}</span><strong>{orderState === 'held' ? `${timerText} 안에 결제를 완료해 주세요.` : orderState === 'confirmed' ? '주문번호 ST-DEMO-20261104' : '객실 재고를 다시 조회해야 합니다.'}</strong></div>
            <StatusPill tone={orderState === 'confirmed' ? 'success' : orderState === 'expired' ? 'danger' : 'warning'}>{orderState === 'confirmed' ? 'CONFIRMED' : orderState === 'expired' ? 'EXPIRED' : 'PENDING_PAYMENT'}</StatusPill>
          </section>
        )}

        <section className="booking-gallery">
          <StayArtwork theme={stay.theme} label={`${stay.name} 대표 이미지`} />
          <div className="booking-gallery__small"><StayArtwork theme={stay.theme} label={`${stay.name} 객실 이미지`} compact /><StayArtwork theme={stay.theme === 'ocean' ? 'night' : 'ocean'} label={`${stay.name} 주변 이미지`} compact /></div>
          <div className="booking-gallery__badge">업주 등록 사진 · 샘플 이미지 영역</div>
        </section>

        <div className="booking-title-row">
          <div><p>{stay.type} · {stay.area} · {stay.source}</p><h1>{stay.name}</h1><span>★ {stay.rating} ({stay.reviewCount}) · {stay.address}</span></div>
          <a href="#booking-rooms">객실 선택하기</a>
        </div>

        <div className="booking-layout">
          <div className="booking-content">
            <section className="booking-section booking-about">
              <div className="booking-section__head"><div><span>STAY INFORMATION</span><h2>숙소 정보</h2></div><StatusPill tone="success">운영 중 · 예약 가능</StatusPill></div>
              <p>{stay.description}</p>
              <dl><div><dt>체크인 / 체크아웃</dt><dd>{stay.checkIn} / {stay.checkOut}</dd></div><div><dt>주차 안내</dt><dd>{stay.parking}</dd></div><div><dt>연락처</dt><dd>{stay.phone}</dd></div><div><dt>홈페이지</dt><dd>{stay.website || '등록 정보 없음'}</dd></div></dl>
              <div className="booking-amenities">{stay.amenities.map((item) => <span key={item}>✓ {item}</span>)}</div>
            </section>

            <section className="booking-section booking-map-section">
              <div className="booking-section__head"><div><span>LOCATION</span><h2>위치와 이동 정보</h2></div></div>
              <div className="booking-detail-map"><span className="booking-detail-map__pin">{stay.name}</span><i className="road-one" /><i className="road-two" /><b>바다</b></div>
              <p>카카오 지도와 코스 이동 시간은 API 연결 후 표시합니다. 현재는 숙소 위치 UI 영역입니다.</p>
            </section>

            <section id="booking-rooms" className="booking-section">
              <div className="booking-section__head"><div><span>ROOM &amp; RATE</span><h2>객실 선택</h2></div><span className="booking-section__hint">{guestCount}명 기준 · {nights || 0}박</span></div>
              <div className="booking-room-list">
                {stay.rooms.map((item) => {
                  const unavailable = guestCount > item.maxGuests || !item.remaining;
                  return (
                    <article key={item.id} className={`booking-room${roomId === item.id ? ' is-selected' : ''}${unavailable ? ' is-disabled' : ''}`}>
                      <StayArtwork theme={stay.theme} label={`${item.name} 객실 이미지`} compact />
                      <div className="booking-room__body"><div className="booking-room__title"><div><h3>{item.name}</h3><p>기준 {item.basis}명 · 최대 {item.maxGuests}명 · {item.area} · {item.beds}</p></div><StatusPill tone={item.remaining <= 1 ? 'warning' : 'success'}>{item.remaining ? `잔여 ${item.remaining}실` : '판매 완료'}</StatusPill></div><div className="booking-room__chips">{item.amenities.map((value) => <span key={value}>{value}</span>)}</div><div className="booking-room__bottom"><div><small>1박</small><strong>{formatWon(item.price)}</strong></div><button type="button" disabled={unavailable} onClick={() => { setRoomId(item.id); setNotice(''); }}>{unavailable ? guestCount > item.maxGuests ? '정원 초과' : '예약 불가' : roomId === item.id ? '선택됨' : '이 객실 선택'}</button></div></div>
                    </article>
                  );
                })}
              </div>
            </section>

            <section className="booking-section booking-course">
              <div className="booking-section__head"><div><span>TRAVEL COURSE</span><h2>숙소 기준 코스</h2></div></div>
              <div className="booking-choice-grid">
                <button type="button" className={courseMode === 'before' ? 'is-selected' : ''} onClick={() => setCourseMode('before')}><strong>코스를 먼저 확인할게요</strong><span>체크인·체크아웃을 반영한 후보 일정과 지도를 확인한 뒤 예약합니다.</span></button>
                <button type="button" className={courseMode === 'after' ? 'is-selected' : ''} onClick={() => setCourseMode('after')}><strong>코스 없이 예약할게요</strong><span>숙소 예약을 먼저 완료하고, 예약 후 코스를 생성합니다.</span></button>
              </div>
              <p className="booking-course__note">코스 미리보기·선택만으로 내 코스에 저장되지 않으며, 예약 성공 후 확정 버전으로 저장됩니다.</p>
            </section>

            <section className="booking-section booking-rules"><div className="booking-section__head"><div><span>POLICY</span><h2>취소·환불 안내</h2></div></div><p>{stay.cancellation}</p><ul><li>실제 환불 금액과 재고 복구 시점은 서버의 예약·결제 결과를 기준으로 합니다.</li><li>결제 실패·취소·임시 점유 만료 시 예약 확정으로 표시하지 않습니다.</li><li>날짜별 확정 요금과 취소 규정은 주문 생성 시점의 내용으로 보관됩니다.</li></ul></section>
          </div>

          <aside className="booking-sidebar">
            <form onSubmit={createOrder}>
              <div className="booking-sidebar__head"><div><span>예약 정보</span><strong>{formatWon(room?.price || stay.price)}<small> / 1박</small></strong></div><select aria-label="숙소 선택" value={stayId} onChange={(event) => setStayId(event.target.value)}>{stays.filter((item) => item.saleable).map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
              <div className="booking-date-grid"><label>체크인<input type="date" value={dates.checkIn} onChange={(event) => { setDates({ ...dates, checkIn: event.target.value }); setOrderState('editing'); }} /></label><label>체크아웃<input type="date" min={dates.checkIn} value={dates.checkOut} onChange={(event) => { setDates({ ...dates, checkOut: event.target.value }); setOrderState('editing'); }} /></label></div>
              <div className="booking-guest-grid"><label>성인<input type="number" min="1" max="10" value={guests.adults} onChange={(event) => setGuests({ ...guests, adults: Number(event.target.value) })} /></label><label>아동<input type="number" min="0" max="8" value={guests.children} onChange={(event) => setGuests({ ...guests, children: Number(event.target.value) })} /></label></div>
              <label className="booking-form-field">예약자 이름<input value={booker.name} onChange={(event) => setBooker({ ...booker, name: event.target.value })} placeholder="실제 투숙자 이름" /></label>
              <label className="booking-form-field">연락처<input value={booker.phone} onChange={(event) => setBooker({ ...booker, phone: event.target.value })} placeholder="010-0000-0000" /></label>
              <label className="booking-form-field">요청사항<textarea rows="3" value={booker.request} onChange={(event) => setBooker({ ...booker, request: event.target.value })} placeholder="숙소에 전달할 요청사항" /></label>
              <PriceSummary room={room} nights={nights} />
              <label className="booking-agreement"><input type="checkbox" checked={agreed} onChange={(event) => setAgreed(event.target.checked)} /><span>취소·환불 규정과 개인정보 제공 내용을 확인했습니다.</span></label>
              {notice && <p className="booking-notice" role={notice.includes('해 주세요') ? 'alert' : 'status'}>{notice}</p>}
              {orderState === 'held' ? <button type="button" className="booking-pay-button" onClick={finishDemoPayment}>테스트 결제 완료 처리</button> : orderState === 'confirmed' ? <a className="booking-pay-button is-link" href="#/">여행 홈으로</a> : <button type="submit" className="booking-pay-button">예약 내용 확인</button>}
              <p className="booking-demo-note">PG 결제 전 프론트엔드 데모입니다. 버튼을 눌러도 실제 결제는 발생하지 않습니다.</p>
            </form>
          </aside>
        </div>
      </main>
      <StayFooter />
    </div>
  );
}
