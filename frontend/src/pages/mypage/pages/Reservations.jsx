import { useEffect, useMemo, useRef, useState } from 'react';
import TourPhoto from '../../../components/media/TourPhoto';
import AccessNotice from '../components/AccessNotice';
import MyCourseMap from '../components/MyCourseMap';
import { myPageApi } from '../api/myPageApi';
import { bookingCategory, isVisibleBooking } from '../data/visibleItems';
import useBookingDate from '../hooks/useBookingDate';
import { money } from '../utils/format';
import tripCover from '../utils/tripCover';
import '../../../css/mypage.css';
import './my-courses.css';
function StayPhoto({ url, name, load }) {
  return <TourPhoto localUrl={url} load={load} alt={`${name || '숙소'} 사진`}
    missing="등록된 숙소 사진이 없습니다." />;
}
function BookingStayInfo({ booking }) {
 const stay = booking.stayInfo;
 if (!stay) return <p className="note">지역에 맞는 숙소 정보를 찾지 못했습니다.</p>;
 return <div className="reservation-stay-info">
  {stay.regionMatched && <p className="note">여행 지역에 맞춘 숙소 데이터입니다. 기존 예약의 일정과 금액은 유지됩니다.</p>}
  <dl>{stay.address && <><dt>숙소 주소</dt><dd>{stay.address}</dd></>}
   {stay.phone && <><dt>숙소 연락처</dt><dd>{stay.phone}</dd></>}
   {stay.checkinTime && <><dt>체크인 / 체크아웃</dt><dd>{stay.checkinTime.slice(0,5)} / {stay.checkoutTime?.slice(0,5) || '미등록'}</dd></>}
  </dl>
  {stay.description && <p className="chat-text">{stay.description}</p>}
 </div>;
}
const tabs=[['all','전체 예약'],['upcoming','예약된 여행'],['past','지난 여행'],['cancelled','취소된 여행']];
const categoryLabels=Object.fromEntries(tabs);
const categoryOrder={upcoming:0,cancelled:1,past:2};
const labels={COMPLETED:'숙박 완료',CONFIRMED:'예약',PENDING_PAYMENT:'결제 대기',CANCELLED:'예약 취소',REFUNDING:'환불 처리 중',REFUND_FAILED:'환불 실패'};
// 연결된 일정의 대표 사진을 우선 사용한다.
const cover = (course, booking) => course?.coverImageUrl || booking.coverImageUrl || tripCover(course?.region || booking.stay || '');

function BookingItinerary({ booking, course, memberId, loadStayPhotos }) {
  const stay = booking.itinerary?.stay;
  const extraPlaces = useMemo(() => {
    const info = booking.stayInfo;
    const location = (info?.regionMatched ? info : stay) || info;
    const name = (info?.regionMatched ? booking.stay : stay?.name) || booking.stay;
    if (!location || !name) return [];
    return [{ id: `reserved-stay-${booking.id}`, kind: 'stay', name,
      address: location.address, latitude: location.latitude, longitude: location.longitude }];
  }, [booking.id, booking.stay, booking.stayInfo, stay]);
  const hasCourse = Boolean(course || booking.courseId);
  if (!hasCourse && !extraPlaces.length) return null;
  const title = hasCourse ? (extraPlaces.length ? '여행 일정과 숙소 위치' : '여행 일정') : '숙소 위치';
  return <MyCourseMap course={course} courseId={booking.courseId} memberId={memberId}
    expanded title={title} hideMapControls showPlaceDetails extraPlaces={extraPlaces} extraTitle="예약한 숙소"
    renderExtraPlaceDetails={place => <StayPhoto url={booking.stayInfo?.imageUrl} name={place.name} load={loadStayPhotos} />} />;
}

function ReservationDetail({ booking, course, memberId, today }) {
  const cancelDialog = useRef(null);
  const cancellationInFlight = useRef(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelResult, setCancelResult] = useState(null);
  const [cancelError, setCancelError] = useState('');
  const bookingStatus = cancelResult ?? booking.status;
  const badge = categoryLabels[bookingCategory({ ...booking, status: bookingStatus }, today)];
  const canRetry = bookingStatus === 'REFUND_FAILED' || bookingStatus === 'REFUNDING';
  const canCancel = canRetry || (bookingStatus === 'CONFIRMED' && booking.date >= today);
  const loadStayPhotos = useMemo(() => {
    let photos;
    return () => photos ??= myPageApi.reservationTourPhotos(booking.id).catch(error => {
      photos = undefined;
      throw error;
    });
  }, [booking.id]);

  async function cancelReservation() {
    if (cancellationInFlight.current || !canCancel) return;
    cancellationInFlight.current = true;
    setCancelling(true);
    setCancelError('');
    try {
      const result = await myPageApi.cancelReservation(booking.id);
      setCancelResult(result.status);
      cancelDialog.current?.close();
    } catch (error) {
      setCancelError(error.message || '예약을 취소하지 못했어요. 다시 시도해 주세요.');
    } finally {
      cancellationInFlight.current = false;
      setCancelling(false);
    }
  }
  return <div className="mypage trips-page trip-detail-page">
    <a className="trips-back trips-back-pill" href="#/reservations">← 내 예약 목록</a>
    <header className="trip-detail-hero">
      <img src={cover(course, booking)} alt="" />
      <div><span className="trip-status">{badge}</span><h1>{course?.title || booking.stay}</h1><p>{booking.date} ~ {booking.endDate}</p></div>
    </header>
    <section className="card reservation-detail-layout" style={{ marginTop: 24 }}>
      <div className="reservation-detail-info">
        <h2>예약 상세</h2>
        <dl>
          <dt>예약번호</dt><dd>{booking.reservationNo}</dd>
          <dt>예약 상태</dt><dd>{labels[bookingStatus] || bookingStatus}</dd>
          <dt>숙소</dt><dd>{booking.stay || '지역에 맞는 숙소 정보 없음'}</dd>
          <dt>객실</dt><dd>{booking.room || '객실 정보 미등록'}</dd>
          <dt>숙박 기간</dt><dd>{booking.date} ~ {booking.endDate}</dd>
          <dt>인원</dt><dd>{booking.guests}명</dd>
          <dt>예약 금액</dt><dd>{money(booking.price)}</dd>
        </dl>
        <BookingStayInfo booking={booking} />
        <div className="actions">
          {canCancel && <button type="button" className="reservation-cancel" onClick={() => {
            setCancelError('');
            cancelDialog.current.showModal();
          }}>{canRetry ? '환불 다시 시도' : '예약취소'}</button>}
          {bookingStatus === 'COMPLETED' && <a className="secondary"
            href={`#/human-courses/create?reservationId=${encodeURIComponent(booking.id)}&from=reservations`}>내 여행 추천 작성·수정</a>}
        </div>
        {cancelResult === 'CANCELLED' && <p role="status">예약을 취소했고 토스 테스트 결제를 전액 환불했어요.</p>}
        {cancelResult === 'REFUND_FAILED' && <p role="alert">토스 환불에 실패했어요. 환불을 다시 시도해 주세요.</p>}
        <dialog ref={cancelDialog} className="reservation-cancel-dialog" aria-labelledby="reservation-cancel-title"
          onCancel={event => { if (cancellationInFlight.current) event.preventDefault(); }}>
          <h2 id="reservation-cancel-title">예약을 취소할까요?</h2>
          <p>예약을 취소하면 토스 테스트 결제 금액 {money(booking.price)}이 전액 환불됩니다.</p>
          {cancelError && <p role="alert">{cancelError}</p>}
          <div className="actions">
            <button type="button" className="secondary" disabled={cancelling} onClick={() => cancelDialog.current.close()}>돌아가기</button>
            <button type="button" className="reservation-cancel" disabled={cancelling} onClick={cancelReservation}>
              {cancelling ? '환불 처리 중…' : '예약 취소 및 환불'}
            </button>
          </div>
        </dialog>
      </div>
      <aside className="reservation-stay-photo"><h3>숙소 사진</h3>
        <StayPhoto url={booking.stayInfo?.imageUrl} name={booking.stay} load={loadStayPhotos} />
      </aside>
    </section>
    <BookingItinerary booking={booking} course={course} memberId={memberId} loadStayPhotos={loadStayPhotos} />
    <div className="trip-detail-footer"><a className="trips-back trips-back-pill" href="#/reservations">← 내 예약 목록</a></div>
  </div>;
}

export default function Reservations({bookings=[],courses=[],reservationId,member}) {
 const [tab,setTab]=useState('all'),[query,setQuery]=useState(''),[latest,setLatest]=useState(null),[status,setStatus]=useState('loading');
 useEffect(()=>{let cancelled=false;setLatest(null);if(!member?.memberId)return;setStatus('loading');myPageApi.load(member.memberId).then(data=>{if(cancelled)return;if(data.memberId!==member.memberId)throw new Error('Member mismatch');setLatest(data);setStatus('ready');}).catch(()=>{if(!cancelled)setStatus('error');});return()=>{cancelled=true;};},[member?.memberId,tab,reservationId]);
 const rows=(latest?.bookings??bookings).filter(isVisibleBooking), plans=latest?.courses??courses;
 const courseFor=b=>b.itinerary?.course ?? plans.find(c=>c.id===b.courseId);
 const today=useBookingDate();
 const category=b=>bookingCategory(b,today);
 const badge=b=>categoryLabels[category(b)];
 if (reservationId) {
  const booking = rows.find(row => String(row.id) === String(reservationId));
  if (!booking) return status === 'loading' ? <p role="status">예약을 불러오는 중입니다.</p>
    : <AccessNotice kind={status === 'error' ? 'unavailable' : 'denied'} />;
  return <ReservationDetail key={booking.id} booking={booking} course={courseFor(booking)}
    memberId={member?.memberId} today={today} />;
 }
 const list=rows.filter(b=>(tab==='all'||category(b)===tab)&&`${courseFor(b)?.title||''} ${courseFor(b)?.region||''} ${b.stay||''} ${b.reservationNo}`.toLowerCase().includes(query.trim().toLowerCase()));
 list.sort((a, b) => {
  const firstCategory = category(a);
  const groupOrder = categoryOrder[firstCategory] - categoryOrder[category(b)];
  if (groupOrder !== 0) return groupOrder;

  // 시작일이 없는 여행은 각 그룹의 맨 뒤에 표시한다.
  if (!a.date && b.date) return 1;
  if (a.date && !b.date) return -1;

  const dateOrder = (a.date || '').localeCompare(b.date || '');
  if (dateOrder !== 0) return firstCategory === 'upcoming' ? dateOrder : -dateOrder;
  return String(a.id).localeCompare(String(b.id), undefined, { numeric: true });
 });
 return <div className="mypage trips-page"><a className="trips-back trips-back-pill" href="#/mypage">← 마이페이지</a><header className="trips-heading"><div><h1>내 예약</h1><p>여행별 예약을 모아 보고, 숙소와 상세 일정을 확인하세요.</p></div></header><div className="trips-list-toolbar"><nav className="trips-filters" aria-label="예약 분류">{tabs.map(([value,label])=><button type="button" key={value} aria-pressed={tab===value} onClick={()=>setTab(value)}>{label}</button>)}</nav><div className="trips-tools"><label className="trips-search"><span className="trips-sr-only">예약 제목, 지역, 숙소 또는 예약번호 검색</span><span aria-hidden="true">⌕</span><input type="search" placeholder="내 예약 검색" value={query} onChange={e=>setQuery(e.target.value)}/></label></div></div><p className="trips-count" role="status">{list.length}개의 예약</p>{status==='loading'&&<p role="status">최신 예약을 불러오는 중입니다.</p>}{status==='error'&&<p role="alert">최신 예약을 불러오지 못했습니다. 다른 분류를 선택해 다시 시도해 주세요.</p>}{!list.length&&status==='ready'&&<p className="empty">조건에 맞는 예약이 없습니다.</p>}<div className="trips-grid">{list.map(b=>{const c=courseFor(b);return <a key={b.id} className="trip-cover-card" href={`#/reservations/${encodeURIComponent(b.id)}`}><img src={cover(c, b)} alt="" loading="lazy"/><span className="trip-status">{badge(b)}</span><div className="trip-card-copy"><span className="trip-region">{c?.region||labels[b.status]||b.status}</span><h2>{c?.title||b.stay||'나의 예약'}</h2><p>{b.date} ~ {b.endDate}<br/>{b.stay}<br/>{b.reservationNo}</p><span className="trip-card-open">예약·일정 자세히 보기 <span aria-hidden="true">↗</span></span></div></a>;})}</div></div>;
}
