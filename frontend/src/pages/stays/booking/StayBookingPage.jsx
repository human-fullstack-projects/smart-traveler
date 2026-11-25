import { useEffect, useMemo, useRef, useState } from 'react';
import { useAuth } from '../../../contexts/AuthContext.jsx';
import OwnerActionNotice from '../../../components/OwnerActionNotice.jsx';
import ItineraryPlaceList from '../../../components/map/ItineraryPlaceList.jsx';
import { myPageApi } from '../../mypage/api/myPageApi.js';
import { getRooms, getStay, getStayTourPhotos } from '../shared/stayApi.js';
import useStayCourse, { readStayCourseId } from '../shared/useStayCourse.js';
import '../../../css/stay-shared.css';
import { formatWon, getDefaultStayDates, getNightCount, getStayCourseMessage, stayTypeLabels } from '../shared/stayData.js';
import { StayLocation, StayOverview } from './StayDetails.jsx';
import StayPhotoGallery from './StayPhotoGallery.jsx';
import StayReservationDialog from './StayReservationDialog.jsx';
import { buildStayGallery } from './stayPhotos.js';
import '../../../css/stay-booking.css';

export default function StayBookingPage({ onStart, busy = false }) {
  const query = new URLSearchParams(window.location.hash.split('?')[1]);
  const lodgingId = query.get('stay');
  const courseId = readStayCourseId(query);
  const { course, route: courseRoute, loading: stayCourseLoading, error: stayCourseError, sourceLabel, retry: retryStayCourse } =
    useStayCourse(courseId, { includePlaceAddresses: true });
  const backToSearch = courseId ? `#/stays?courseId=${encodeURIComponent(courseId)}` : '#/stays';
  const defaults = getDefaultStayDates();
  const { member, authChecked, authCheckFailed } = useAuth();
  const isOwner = member?.roleCode === 'OWNER';
  const [ownerNotice, setOwnerNotice] = useState(false);
  const [checkIn, setCheckIn] = useState(query.get('checkIn') || defaults.checkIn);
  const [checkOut, setCheckOut] = useState(query.get('checkOut') || defaults.checkOut);
  const [guests, setGuests] = useState(Number(query.get('adults') || 2) + Number(query.get('children') || 0));
  const [stay, setStay] = useState(null);
  const [tourGallery, setTourGallery] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [roomId, setRoomId] = useState(null);
  const [reservationOpen, setReservationOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [refresh, setRefresh] = useState(0);
  const [startingCourse, setStartingCourse] = useState(false);
  const [courseError, setCourseError] = useState('');
  const [placeSelection, setPlaceSelection] = useState(null);
  const selectedPlaceId = placeSelection?.course === course ? placeSelection?.id : null;
  const courseInFlight = useRef(false);
  const courseMessage = getStayCourseMessage(stay?.sidoName, stay?.name);
  const room = rooms.find((item) => item.roomId === roomId && item.available !== false);
  const nights = getNightCount(checkIn, checkOut);
  const photos = useMemo(() => buildStayGallery(stay, rooms, tourGallery?.data), [stay, rooms, tourGallery]);

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
    setReservationOpen(false);
    setRoomId(null);
    setRooms([]);
    setNotice('');
    setter(value);
  }

  function openReservation(selectedRoomId) {
    if (isOwner) {
      setOwnerNotice(true);
      return;
    }
    setRoomId(selectedRoomId);
    setNotice('');
    setReservationOpen(true);
  }

  function reloadRoomsAfterConflict() {
    setReservationOpen(false);
    setRoomId(null);
    setRooms([]);
    setNotice('객실 재고 또는 요금이 변경되었습니다. 다시 선택해 주세요.');
    setRefresh(value => value + 1);
  }

  function selectCoursePlace(place) {
    setPlaceSelection(current => current?.course === course && current?.id === place.id ? null : { course, id: place.id });
  }

  async function startCourse() {
    if (!onStart || !courseMessage || !authChecked || authCheckFailed || busy || courseInFlight.current) return;
    courseInFlight.current = true;
    setStartingCourse(true);
    setCourseError('');
    try {
      await onStart(courseMessage);
    } catch {
      setCourseError('대화를 시작하지 못했어요. 잠시 후 다시 시도해 주세요.');
    } finally {
      courseInFlight.current = false;
      setStartingCourse(false);
    }
  }

  return (
    <div className="stay-ui stay-booking-page">
      {ownerNotice && isOwner && <OwnerActionNotice />}
      <main className="booking-main">
        <p><a href={backToSearch}>← 숙소 검색</a></p>
        {!lodgingId && <p>숙소 검색에서 예약할 숙소를 선택해 주세요.</p>}
        {!stay && notice && <p className="booking-notice" role="alert">{notice}</p>}
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
          <div className="booking-layout">
            <section className="booking-section booking-rooms">
              <h2>객실 선택</h2>
              <div className="booking-date-grid">
                <label>체크인<input type="date" value={checkIn} onChange={(event) => changeCondition(setCheckIn, event.target.value)} /></label>
                <label>체크아웃<input type="date" value={checkOut} min={checkIn} onChange={(event) => changeCondition(setCheckOut, event.target.value)} /></label>
              </div>
              <label className="booking-form-field">인원<input type="number" min="1" max="18" value={guests} onChange={(event) => changeCondition(setGuests, Number(event.target.value))} /></label>
              {notice && <p className="booking-notice" role="alert">{notice}</p>}
              {loadingRooms && <p role="status">객실을 조회하고 있습니다.</p>}
              {!loadingRooms && !nights && <p>체크인보다 늦은 체크아웃 날짜를 선택해 주세요.</p>}
              {!loadingRooms && nights > 0 && !notice && !rooms.some((item) => item.available !== false) && <p>선택한 조건에 예약할 수 있는 객실이 없습니다.</p>}
              <div className="booking-room-list">
                {rooms.map((item, index) => (
                  <article key={item.roomId} className={'booking-room' + (reservationOpen && roomId === item.roomId ? ' is-selected' : '') + (item.available === false ? ' is-sold-out' : '')}>
                    <StayPhotoGallery title={item.name} photos={photos.roomGalleries[index].photos} room />
                    <div className="booking-room__body">
                      <h3>{item.name}</h3>
                      <p>최대 {item.maxCapacity}명 · {item.available === false ? '객실 없음' : `잔여 ${item.remaining}실`}</p>
                      <div className="booking-room__bottom">
                        <strong>{item.available === false ? '객실 없음' : `${nights}박 ${formatWon(item.totalPrice)}`}</strong>
                        <button type="button" aria-haspopup="dialog" disabled={item.available === false} onClick={() => openReservation(item.roomId)}>
                          {item.available === false ? '객실 없음' : '객실 예약'}
                        </button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
            <StayOverview stay={stay} />
          </div>
          <StayLocation stay={stay} courseRoute={courseId ? courseRoute : null} selectedPlaceId={selectedPlaceId}
            onCoursePlaceSelect={course ? selectCoursePlace : undefined} />
          {course && courseRoute.days.some(day => day.places.length) && <section className="booking-section booking-course-itinerary itinerary-map" aria-labelledby="booking-itinerary-title">
            <h2 id="booking-itinerary-title">여행 일정</h2>
            <ItineraryPlaceList days={courseRoute.days} selectedId={selectedPlaceId} onSelect={selectCoursePlace}
              loadDetails={myPageApi.placeDetails} loadPhotos={myPageApi.placeTourPhotos} searchRegion={course.region} />
          </section>}
          {room && !isOwner && <StayReservationDialog key={`${room.roomId}-${member?.memberId ?? 'guest'}`} open={reservationOpen} room={room}
            member={member} checkIn={checkIn} checkOut={checkOut} guests={guests} courseId={course?.id} waitingForCourse={Boolean(courseId && !course)}
            onClose={() => setReservationOpen(false)} onAvailabilityChanged={reloadRoomsAfterConflict} />}
        </>}
      </main>
    </div>
  );
}
