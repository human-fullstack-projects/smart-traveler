import { useEffect, useMemo, useRef, useState } from 'react';
import { useAuth } from '../../../contexts/AuthContext.jsx';
import { createReservation, getRooms, getStay, getStayTourPhotos } from '../shared/stayApi.js';
import useStayCourse, { readStayCourseId } from '../shared/useStayCourse.js';
import '../../../css/stay-shared.css';
import { formatWon, getDefaultStayDates, getNightCount, stayTypeLabels } from '../shared/stayData.js';
import { StayLocation, StayOverview } from './StayDetails.jsx';
import StayPhotoGallery from './StayPhotoGallery.jsx';
import { buildStayGallery } from './stayPhotos.js';
import '../../../css/stay-booking.css';

export default function StayBookingPage({ onStart, busy = false }) {
  const query = new URLSearchParams(window.location.hash.split('?')[1]);
  const lodgingId = query.get('stay');
  const courseId = readStayCourseId(query);
  const { course, route: courseRoute, loading: stayCourseLoading, error: stayCourseError, sourceLabel, retry: retryStayCourse } = useStayCourse(courseId);
  const backToSearch = courseId ? `#/stays?courseId=${encodeURIComponent(courseId)}` : '#/stays';
  const defaults = getDefaultStayDates();
  const { member, authChecked, authCheckFailed } = useAuth();
  const [checkIn, setCheckIn] = useState(query.get('checkIn') || defaults.checkIn);
  const [checkOut, setCheckOut] = useState(query.get('checkOut') || defaults.checkOut);
  const [guests, setGuests] = useState(Number(query.get('adults') || 2) + Number(query.get('children') || 0));
  const [stay, setStay] = useState(null);
  const [tourGallery, setTourGallery] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [roomId, setRoomId] = useState(null);
  const [notice, setNotice] = useState('');
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [saving, setSaving] = useState(false);
  const [refresh, setRefresh] = useState(0);
  const [visitorName, setVisitorName] = useState('');
  const [sameAsBooker, setSameAsBooker] = useState(false);
  const [startingCourse, setStartingCourse] = useState(false);
  const [courseError, setCourseError] = useState('');
  const courseInFlight = useRef(false);
  const courseMessage = stay?.name?.trim();
  const bookerName = member?.nickname?.trim() || member?.loginId || '';
  const bookerEmail = member?.email?.trim() || '';
  const room = rooms.find((item) => item.roomId === roomId && item.available !== false);
  const nights = getNightCount(checkIn, checkOut);
  const photos = useMemo(() => buildStayGallery(stay, rooms, tourGallery?.data), [stay, rooms, tourGallery]);

  useEffect(() => {
    setSameAsBooker(false);
    setVisitorName('');
  }, [member?.memberId]);

  useEffect(() => {
    if (!lodgingId) return;
    let active = true;
    getStay(lodgingId)
      .then((data) => { if (active) setStay(data); })
      .catch((error) => { if (active) setNotice(error.status === 404 ? '숙소를 찾을 수 없습니다.' : error.message); });
    return () => { active = false; };
  }, [lodgingId]);

  useEffect(() => {
    if (!stay?.lodgingId) return;
    let active = true;
    setTourGallery(null);
    getStayTourPhotos(stay.lodgingId)
      .then(data => { if (active) setTourGallery({ status: 'ready', data }); })
      .catch(() => { if (active) setTourGallery({ status: 'error' }); });
    return () => { active = false; };
  }, [stay?.lodgingId]);

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

  async function startCourse() {
    if (!onStart || !courseMessage || !authChecked || authCheckFailed || busy || courseInFlight.current) return;
    courseInFlight.current = true;
    setStartingCourse(true);
    setCourseError('');
    try {
      await onStart(courseMessage, stay.regionId ?? null, stay.lodgingId);
    } catch {
      setCourseError('대화를 시작하지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      courseInFlight.current = false;
      setStartingCourse(false);
    }
  }

  async function submit(event) {
    event.preventDefault();
    if (!room || (courseId && !course) || !bookerEmail || (!sameAsBooker && !visitorName.trim()) || saving) return;
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setNotice('');
    try {
      const reservation = await createReservation({
        roomId: room.roomId, checkIn, checkOut, guestCount: guests,
        visitorName: sameAsBooker ? null : visitorName.trim(),
        requestMemo: form.get('requestMemo'), agreedToPolicy: form.has('agreedToPolicy'),
        expectedTotalPrice: room.totalPrice,
        ...(course ? { courseId: course.id } : {}),
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
      <main className="booking-main">
        <p><a href={backToSearch}>← 숙소 검색</a></p>
        {!lodgingId && <p>숙소 검색에서 예약할 숙소를 선택해 주세요.</p>}
        {notice && <p className="booking-notice" role="alert">{notice}</p>}
        {stayCourseLoading && <p className="booking-notice" role="status">{sourceLabel} 정보를 불러오고 있어요.</p>}
        {stayCourseError && <p className="booking-notice" role="alert">{stayCourseError} <button type="button" onClick={retryStayCourse}>다시 시도</button></p>}
        {course && <p>{sourceLabel} <strong>{course.title}</strong>에 숙소를 선택하고 있어요.</p>}
        {stay && <>
          <header className="booking-title-row">
            <div>
              {stay.lodgingType && <p>{stayTypeLabels[stay.lodgingType] || stay.lodgingType}</p>}
              <h1>{stay.name}</h1>
              {stay.address && <span>{stay.address}</span>}
            </div>
            {onStart && courseMessage && <div className="booking-course-start">
              <button type="button" onClick={startCourse}
                disabled={!authChecked || authCheckFailed || busy || startingCourse}>
                {startingCourse ? '대화 시작 중…' : '이 숙소 주변 여행 코스 짜기'}
              </button>
              {courseError && <p className="booking-notice" role="alert">{courseError}</p>}
            </div>}
          </header>
          <StayPhotoGallery title={stay.name} stayId={stay.lodgingId} cover={photos.cover} photos={photos.photos}
            photoStatus={tourGallery?.status || 'loading'} />
          <div className="booking-stay-info">
            <StayOverview stay={stay} />
            <StayLocation stay={stay} courseRoute={courseId ? courseRoute : null} />
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
              {!loadingRooms && nights > 0 && !notice && !rooms.some((item) => item.available !== false) && <p>선택한 조건에 예약할 수 있는 객실이 없습니다.</p>}
              <div className="booking-room-list">
                {rooms.map((item, index) => (
                  <article key={item.roomId} className={'booking-room' + (roomId === item.roomId ? ' is-selected' : '') + (item.available === false ? ' is-sold-out' : '')}>
                    <StayPhotoGallery title={item.name} photos={photos.roomGalleries[index].photos} room />
                    <div className="booking-room__body">
                      <h3>{item.name}</h3>
                      <p>최대 {item.maxCapacity}명 · {item.available === false ? '객실 없음' : `잔여 ${item.remaining}실`}</p>
                      <div className="booking-room__bottom">
                        <strong>{item.available === false ? '객실 없음' : `${nights}박 ${formatWon(item.totalPrice)}`}</strong>
                        {/* 피드백: 재고가 없으면 「객실 없음」으로 표시하고 선택을 막는다 */}
                        <button type="button" disabled={saving || item.available === false} onClick={() => { setRoomId(item.roomId); setNotice(''); }}>
                          {item.available === false ? '객실 없음' : roomId === item.roomId ? '선택됨' : '객실 선택'}
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
                <label className="booking-form-field">예약자 이름<input value={bookerName} disabled /></label>
                <label className="booking-form-field">연락처 (이메일)<input type="email" value={bookerEmail} disabled /></label>
                {member && !bookerEmail && <p className="booking-contact-note">예약하려면 <a href="#/my-profile">내 정보</a>에 이메일을 등록해 주세요.</p>}
                <label className="booking-agreement booking-same-visitor">
                  <input type="checkbox" checked={sameAsBooker} disabled={!member || saving} onChange={(event) => {
                    setSameAsBooker(event.target.checked);
                    setVisitorName('');
                  }} />예약자와 동일
                </label>
                <label className="booking-form-field">방문자 이름<input name="visitorName" maxLength={50} autoComplete="off"
                  required={!sameAsBooker} value={sameAsBooker ? bookerName : visitorName}
                  disabled={!member || sameAsBooker || saving} onChange={(event) => setVisitorName(event.target.value)} /></label>
                <label className="booking-form-field">요청사항<textarea name="requestMemo" rows="3" maxLength="500" /></label>
                <label className="booking-agreement"><input name="agreedToPolicy" type="checkbox" required />예약자와 방문자 정보가 숙소에 전달되며, 10분 이내에 테스트 결제를 진행해야 함을 확인했습니다.</label>
                {member
                  ? <button className="booking-pay-button" disabled={!room || !bookerEmail || saving || Boolean(courseId && !course)} type="submit">{saving ? '예약 중…' : '예약하고 결제하기'}</button>
                  : <a className="booking-pay-button is-link" href="#/login">로그인하고 예약하기</a>}
              </form>
            </aside>
          </div>
        </>}
      </main>
    </div>
  );
}
