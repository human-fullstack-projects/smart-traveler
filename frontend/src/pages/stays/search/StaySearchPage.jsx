import { useEffect, useMemo, useRef, useState } from 'react';
import { StayArtwork, StatusPill } from '../shared/StayShell.jsx';
import { formatWon, getDefaultStayDates, stayTypeLabels } from '../shared/stayData.js';
import { getStayRegions, searchStays } from '../shared/stayApi.js';
import useStayCourse, { readStayCourseId } from '../shared/useStayCourse.js';
import KakaoMap from '../../../components/map/KakaoMap.jsx';
import { loadKakaoMaps } from '../../../components/map/loadKakaoMaps.js';
import { hasValidCoordinates, resolveDestinationCenter } from '../../../components/map/resolveKakaoPlace.js';
import '../../../css/stay-search.css';
import './stay-kakao-map.css';
import './stay-search-paging.css';

// DB lodging.amenities에 실제로 있는 값 중 많이 쓰는 것
const AMENITY_OPTIONS = ['주차', '와이파이', '취사', '바비큐', '조식', '수영장', '엘리베이터', '넷플릭스'];
const PRICE_MIN = 40000;
const PRICE_MAX = 300000; // 끝까지 올리면 「제한 없음」
const PAGE_SIZES = [5, 10];

function lodgingDates(checkIn, end, today) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(checkIn) || !/^\d{4}-\d{2}-\d{2}$/.test(end)) return null;
  const duration = (Date.parse(end) - Date.parse(checkIn)) / 86400000;
  if (!Number.isFinite(duration) || duration < 0 || duration > 30 || checkIn < today) return null;
  const checkOut = duration === 0 ? new Date(Date.parse(checkIn) + 86400000).toISOString().slice(0, 10) : end;
  return { checkIn, checkOut };
}

function guestCount(queryValue, courseValue, min, max, fallback) {
  const count = Number(queryValue || courseValue);
  return Number.isInteger(count) && count >= min && count <= max ? count : fallback;
}

/** 목적지 글자(예: "부산 해운대")에 들어 있는 시·도 / 시·군·구를 찾는다. */
function matchRegion(regions, text) {
  if (!text) return { sido: '', sigungu: '' };
  const short = (name) => name.replace(/(통합특별시|특별자치시|특별자치도|특별시|광역시|도)$/, '');
  const namedSido = regions.find((item) => text.includes(item.name) || text.includes(short(item.name)));
  if (namedSido) {
    const child = namedSido.children.find((item) => text.includes(item.name) || text.includes(item.name.replace(/(시|군|구)$/, '')));
    return { sido: String(namedSido.regionId), sigungu: child ? String(child.regionId) : '' };
  }
  for (const sido of regions) {
    const child = sido.children.find((item) => text.includes(item.name) || text.includes(item.name.replace(/(시|군|구)$/, '')));
    if (child) return { sido: String(sido.regionId), sigungu: String(child.regionId) };
  }
  return { sido: '', sigungu: '' };
}

function pageNumbers(page, totalPages) {
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  return Array.from({ length: Math.min(5, totalPages) }, (_, index) => start + index);
}

function regionById(regions, regionId, destination) {
  if (!regionId) return null;
  const id = String(regionId);
  const sido = regions.find(item => String(item.regionId) === id);
  if (sido) {
    const matched = matchRegion([sido], destination);
    return { sido: id, sigungu: matched.sigungu };
  }
  const parent = regions.find(item => item.children.some(child => String(child.regionId) === id));
  return parent ? { sido: String(parent.regionId), sigungu: id } : null;
}

function SearchMap({ items, selectedId, onSelect, query, focus, courseRoute }) {
  const [mapStatus, setMapStatus] = useState('loading');
  const stayPlaces = useMemo(() => items.filter(hasValidCoordinates).map(stay => ({
    id: String(stay.lodgingId), latitude: stay.latitude, longitude: stay.longitude,
    name: stay.name, kind: 'stay', markerLabel: '★',
  })), [items]);
  const places = useMemo(() => courseRoute ? [...stayPlaces, ...courseRoute.places] : stayPlaces, [stayPlaces, courseRoute]);
  const selectedItem = items.find((item) => String(item.lodgingId) === String(selectedId));
  const bookingQuery = selectedId ? new URLSearchParams({ stay: selectedId, ...query }).toString() : '';

  function handleMarker(place) {
    if (place.kind === 'stay') onSelect(place.id);
  }

  return (
    <section className="stay-kakao-map" aria-label={courseRoute ? '숙소와 저장된 여행 일정 지도' : '숙소 검색 결과 지도'}>
      <KakaoMap places={places} paths={courseRoute?.paths} level={9} selectedId={selectedId} fitPoints={places} focus={focus}
        onMarkerClick={handleMarker} onStatusChange={setMapStatus} height="100%" />
      {mapStatus === 'ready' && selectedItem && <div className="stay-kakao-map__details" role="status"><strong>{selectedItem.name}</strong><span>{selectedItem.saleable ? `1박 ${formatWon(selectedItem.minPrice)}부터` : '선택 날짜 판매 없음'}</span><a href={`#/stays/booking?${bookingQuery}`}>객실 보기 →</a></div>}
    </section>
  );
}

function StayCard({ stay, selected, query, onShow }) {
  const [photoFailed, setPhotoFailed] = useState(false);
  const id = String(stay.lodgingId);
  const bookingQuery = new URLSearchParams({ stay: id, ...query }).toString();
  return (
    <article className={`stay-result-card${selected ? ' is-selected' : ''}`} onMouseEnter={() => onShow(stay)}>
      <button type="button" className="stay-result-card__image" onClick={() => onShow(stay, 4)} aria-pressed={selected} aria-label={`${stay.name} 지도에서 보기`}>
        {stay.thumbnailUrl && !photoFailed
          ? <img className="stay-result-card__photo" src={stay.thumbnailUrl} alt={`${stay.name} 사진`} loading="lazy" referrerPolicy="no-referrer" onError={() => setPhotoFailed(true)} />
          : <StayArtwork theme="ocean" label={`${stay.name} 이미지 영역`} compact />}
        {stay.saleable && stay.remaining <= 3 && <span className="stay-result-card__remaining">남은 객실 {stay.remaining}</span>}
      </button>
      <div className="stay-result-card__body">
        <div className="stay-result-card__top">
          <div>
            <p className="stay-result-card__category">{stayTypeLabels[stay.lodgingType] || stay.lodgingType} · {stay.regionName || ''}</p>
            <h2>{stay.name}</h2>
          </div>
        </div>
        <p className="stay-result-card__description">{stay.address}</p>
        <div className="stay-result-card__chips">
          {stay.amenities.slice(0, 4).map((item) => <span key={item}>{item}</span>)}
        </div>
        <div className="stay-result-card__bottom">
          <div>
            <StatusPill tone={stay.saleable ? 'success' : 'warning'}>{stay.saleable ? '예약 가능' : '선택 날짜 판매 없음'}</StatusPill>
          </div>
          {stay.saleable ? (
            <div className="stay-result-card__price">
              <span>1박 평균 최저가</span><strong>{formatWon(stay.minPrice)}</strong>
              <a href={`#/stays/booking?${bookingQuery}`}>객실 보기</a>
            </div>
          ) : (
            <div className="stay-result-card__price is-info"><span>날짜·인원을 바꿔 보세요</span><strong>예약 불가</strong><button type="button" onClick={() => onShow(stay, 4)}>지도에서 보기</button></div>
          )}
        </div>
      </div>
    </article>
  );
}

export default function StaySearchPage() {
  const defaultDates = useMemo(() => getDefaultStayDates(), []);
  const today = useMemo(() => new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' }), []);
  const params = new URLSearchParams(window.location.hash.split('?')[1]);
  const courseId = readStayCourseId(params);
  const { course, route: courseRoute, loading: courseLoading, error: courseError, retry: retryCourse } = useStayCourse(courseId);
  const destinationQuery = course?.region || params.get('destination') || '';
  const sidoQuery = params.get('sido') || '';
  const sigunguQuery = params.get('sigungu') || '';
  const checkInQuery = params.get('checkIn');
  const checkOutQuery = params.get('checkOut');
  const adultsQuery = params.get('adults');
  const childrenQuery = params.get('children');
  const [regions, setRegions] = useState(null);
  const [search, setSearch] = useState({ sido: '', sigungu: '', ...defaultDates, adults: 2, children: 0 });
  const [applied, setApplied] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [appliedKeyword, setAppliedKeyword] = useState('');
  const [types, setTypes] = useState([]);
  const [amenities, setAmenities] = useState([]);
  const [maxPrice, setMaxPrice] = useState(PRICE_MAX);
  const [appliedMaxPrice, setAppliedMaxPrice] = useState(PRICE_MAX);
  const [bookableOnly, setBookableOnly] = useState(false);
  const [sort, setSort] = useState('distance');
  const [size, setSize] = useState(10);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState({ items: [], total: 0, totalPages: 1 });
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState('');
  const [stayError, setStayError] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [destinationCenter, setDestinationCenter] = useState(null);
  const [focus, setFocus] = useState(null);
  const resultsRef = useRef(null);
  const lastCriteriaKey = useRef(null);

  useEffect(() => {
    let active = true;
    getStayRegions().then(data => { if (active) setRegions(data); })
      .catch(error => { if (active) setStayError(error.message); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!regions || (courseId && !course)) return;
    const sidoById = sidoQuery ? regions.find(item => String(item.regionId) === sidoQuery) : null;
    const matched = sidoById
      ? { sido: sidoQuery, sigungu: sidoById.children.some(child => String(child.regionId) === sigunguQuery) ? sigunguQuery : '' }
      : regionById(regions, course?.regionId, destinationQuery) || matchRegion(regions, destinationQuery);
    const busan = regions.find(item => item.name.startsWith('부산'));
    const dates = lodgingDates(checkInQuery, checkOutQuery, today)
      || lodgingDates(course?.startDate, course?.endDate, today);
    const start = {
      sido: matched.sido || (course || destinationQuery || sidoQuery ? '' : String(busan?.regionId || '')),
      sigungu: matched.sigungu,
      ...(dates || defaultDates),
      adults: guestCount(adultsQuery, course?.adults, 1, 10, 2),
      children: guestCount(childrenQuery, course?.children, 0, 8, 0),
    };
    const messages = [];
    if (destinationQuery && !sidoQuery && !matched.sido) messages.push(`'${destinationQuery}' 지역을 찾지 못해 전국 결과를 보여 드려요.`);
    if ((course || checkInQuery || checkOutQuery) && !dates) messages.push('여행 날짜로 예약할 수 없어 기본 숙박 날짜로 검색했어요. 필요하면 날짜를 바꿔 주세요.');
    setNotice(messages.join(' '));
    setSearch(start);
    setApplied(start);
  }, [regions, courseId, course, destinationQuery, sidoQuery, sigunguQuery, checkInQuery, checkOutQuery, adultsQuery, childrenQuery, today, defaultDates]);

  useEffect(() => {
    if (!destinationQuery || courseId) return;
    let cancelled = false;
    loadKakaoMaps().then((maps) => resolveDestinationCenter(maps, destinationQuery))
      .then((center) => { if (!cancelled && center) setDestinationCenter(center); }).catch(() => {});
    return () => { cancelled = true; };
  }, [destinationQuery, courseId]);

  // 가격 막대는 손을 뗀 뒤 0.4초 있다가 적용(움직이는 동안 요청을 줄임)
  useEffect(() => {
    const timer = setTimeout(() => setAppliedMaxPrice(maxPrice), 400);
    return () => clearTimeout(timer);
  }, [maxPrice]);

  const conditions = useMemo(() => (!courseId || course) && applied && ({
    sido: applied.sido || undefined,
    sigungu: applied.sigungu || undefined,
    checkIn: applied.checkIn,
    checkOut: applied.checkOut,
    guests: applied.adults + applied.children,
    types,
    amenities,
    keyword: appliedKeyword.trim() || undefined,
    maxPrice: appliedMaxPrice >= PRICE_MAX ? undefined : appliedMaxPrice,
    bookableOnly: bookableOnly || undefined,
  }), [courseId, course, applied, types, amenities, appliedKeyword, appliedMaxPrice, bookableOnly]);

  // 조건이 바뀌면 1쪽부터
  useEffect(() => { setPage(0); }, [conditions, sort, size]);
  const criteriaKey = useMemo(() => JSON.stringify([conditions, sort, size]), [conditions, sort, size]);

  // 목록: 서버가 조건으로 거르고 한 쪽(5/10곳)만 보냄
  useEffect(() => {
    if (!conditions) return undefined;
    if (lastCriteriaKey.current !== criteriaKey) {
      lastCriteriaKey.current = criteriaKey;
      if (page !== 0) return undefined; // 먼저 1쪽으로 돌아간 뒤 한 번만 요청
    }
    let active = true;
    setLoading(true);
    setStayError('');
    const start = courseRoute.places[0] || destinationCenter;
    const center = start ? { lat: start.latitude, lng: start.longitude } : {};
    searchStays({ ...conditions, sort, page, size, ...center })
      .then((data) => { if (active) { setResult(data); setSelectedId(data.items[0] ? String(data.items[0].lodgingId) : null); } })
      .catch((error) => { if (active) setStayError(error.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [conditions, sort, page, size, destinationCenter, courseRoute, criteriaKey]);

  function toggleFilter(value, setter) {
    setter((current) => current.includes(value) ? current.filter((item) => item !== value) : [...current, value]);
  }

  function submit(event) {
    event.preventDefault();
    if (!regions || (courseId && !course)) return;
    if (!search.checkIn || !search.checkOut || search.checkOut <= search.checkIn) {
      setNotice('체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.');
      return;
    }
    setApplied({ ...search });
    setAppliedKeyword(keyword);
    setNotice('');
  }

  function showOnMap(stay, level) {
    setSelectedId(String(stay.lodgingId));
    if (hasValidCoordinates(stay)) setFocus((current) => ({ latitude: Number(stay.latitude), longitude: Number(stay.longitude), level, key: (current?.key || 0) + 1 }));
  }

  function goPage(next) {
    setPage(next);
    resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  const sido = regions?.find((item) => String(item.regionId) === String(search.sido));
  const appliedSido = applied && regions?.find((item) => String(item.regionId) === String(applied.sido));
  const appliedSigungu = appliedSido?.children.find((item) => String(item.regionId) === String(applied.sigungu));
  const regionLabel = appliedSigungu ? `${appliedSido.name} ${appliedSigungu.name}` : appliedSido?.name || '전국';
  const query = applied ? { checkIn: applied.checkIn, checkOut: applied.checkOut, adults: String(applied.adults), children: String(applied.children), ...(courseId ? { courseId } : {}) } : {};

  return (
    <div className="stay-ui stay-search-page">
      <main className="stay-search-main">
        <section className="stay-search-hero">
          <div><span>Stay Finder</span><h1>여행에 맞는 숙소를 찾아보세요.</h1><p>전국 숙소 중 조건에 맞는 곳을 {size}곳씩 보여 드려요. 선택한 날짜에 판매 중인 객실만 예약할 수 있어요.</p></div>
        </section>

        {course && <p className="stay-search-notice">저장된 여행 <strong>{course.title}</strong>에 숙소를 선택하고 있어요.</p>}
        {courseLoading && <p className="stay-search-notice" role="status">저장된 여행 정보를 불러오고 있어요.</p>}
        {courseError && <p className="stay-search-notice" role="alert">{courseError} <button type="button" onClick={retryCourse}>다시 시도</button> <a href="#/stays">여행 연결 없이 숙소 찾기</a></p>}
        <form className="stay-search-bar stay-search-bar--regions" onSubmit={submit}>
          <label><span>시·도</span><select value={search.sido} onChange={(event) => setSearch({ ...search, sido: event.target.value, sigungu: '' })}><option value="">전국</option>{regions?.map((item) => <option key={item.regionId} value={item.regionId}>{item.name} ({item.count.toLocaleString()})</option>)}</select></label>
          <label><span>시·군·구</span><select value={search.sigungu} disabled={!sido?.children.length} onChange={(event) => setSearch({ ...search, sigungu: event.target.value })}><option value="">전체</option>{(sido?.children || []).map((item) => <option key={item.regionId} value={item.regionId}>{item.name} ({item.count.toLocaleString()})</option>)}</select></label>
          <label><span>체크인</span><input type="date" min={today} value={search.checkIn} onChange={(event) => setSearch({ ...search, checkIn: event.target.value })} /></label>
          <label><span>체크아웃</span><input type="date" min={search.checkIn || today} value={search.checkOut} onChange={(event) => setSearch({ ...search, checkOut: event.target.value })} /></label>
          <label><span>성인</span><input type="number" min="1" max="10" value={search.adults} onChange={(event) => setSearch({ ...search, adults: Math.max(1, Number(event.target.value) || 1) })} /></label>
          <label><span>아동</span><input type="number" min="0" max="8" value={search.children} onChange={(event) => setSearch({ ...search, children: Math.max(0, Number(event.target.value) || 0) })} /></label>
          <button type="submit" disabled={!regions || Boolean(courseId && !course)}>숙소 검색</button>
        </form>
        {notice && <p className="stay-search-notice" role="status">{notice}</p>}
        {stayError && <p className="stay-search-notice" role="alert">{stayError}</p>}

        <div className="stay-search-layout">
          <aside className="stay-filter" aria-label="숙소 검색 필터">
            <div className="stay-filter__head"><h2>필터</h2><button type="button" onClick={() => { setTypes([]); setAmenities([]); setMaxPrice(PRICE_MAX); setBookableOnly(false); setKeyword(''); setAppliedKeyword(''); }}>초기화</button></div>
            <fieldset><legend>숙소 이름</legend><form className="stay-filter__keyword" onSubmit={(event) => { event.preventDefault(); setAppliedKeyword(keyword); }}><input type="search" maxLength={50} placeholder="예: 호텔, 펜션 이름" value={keyword} onChange={(event) => setKeyword(event.target.value)} /><button type="submit">찾기</button></form></fieldset>
            <fieldset><legend>숙소 유형</legend>{Object.entries(stayTypeLabels).map(([code, label]) => <label key={code}><input type="checkbox" checked={types.includes(code)} onChange={() => toggleFilter(code, setTypes)} />{label}</label>)}</fieldset>
            <fieldset><legend>편의시설</legend>{AMENITY_OPTIONS.map((item) => <label key={item}><input type="checkbox" checked={amenities.includes(item)} onChange={() => toggleFilter(item, setAmenities)} />{item}</label>)}</fieldset>
            <fieldset><legend>1박 최대 요금</legend><input className="stay-filter__range" type="range" min={PRICE_MIN} max={PRICE_MAX} step="10000" value={maxPrice} onChange={(event) => setMaxPrice(Number(event.target.value))} /><strong>{maxPrice >= PRICE_MAX ? '제한 없음' : `${formatWon(maxPrice)} 이하`}</strong></fieldset>
            <label className="stay-filter__switch"><input type="checkbox" checked={bookableOnly} onChange={(event) => setBookableOnly(event.target.checked)} /><span />예약 가능 숙소만</label>
          </aside>

          <section className="stay-results" aria-label="숙소 검색 결과" ref={resultsRef}>
            <div className="stay-results__toolbar">
              <p><strong>{regionLabel}</strong> 검색 결과 <b>{result.total.toLocaleString()}</b>곳{loading && <span className="stay-results__loading"> · 불러오는 중…</span>}</p>
              <div className="stay-results__controls">
                <select aria-label="정렬 기준" value={sort} onChange={(event) => setSort(event.target.value)}><option value="distance">가까운 순</option><option value="price">낮은 가격 순</option><option value="rating" disabled>평점 높은 순 (보류)</option><option value="name">이름순</option></select>
                <select aria-label="한 쪽에 보일 숙소 수" value={size} onChange={(event) => setSize(Number(event.target.value))}>{PAGE_SIZES.map((value) => <option key={value} value={value}>{value}개씩</option>)}</select>
              </div>
            </div>
            {result.items.length
              ? result.items.map((stay) => <StayCard key={stay.lodgingId} stay={stay} selected={selectedId === String(stay.lodgingId)} query={query} onShow={showOnMap} />)
              : !loading && <div className="stay-results__empty"><strong>조건에 맞는 숙소가 없습니다.</strong><span>필터를 줄이거나 다른 지역·날짜를 선택해 주세요.</span></div>}
            {result.totalPages > 1 && (
              <nav className="stay-pagination" aria-label="검색 결과 페이지">
                <button type="button" disabled={page === 0} onClick={() => goPage(0)} aria-label="첫 쪽">«</button>
                <button type="button" disabled={page === 0} onClick={() => goPage(page - 1)}>이전</button>
                {pageNumbers(page, result.totalPages).map((number) => <button type="button" key={number} className={number === page ? 'is-current' : ''} aria-current={number === page ? 'page' : undefined} onClick={() => goPage(number)}>{number + 1}</button>)}
                <button type="button" disabled={page >= result.totalPages - 1} onClick={() => goPage(page + 1)}>다음</button>
                <button type="button" disabled={page >= result.totalPages - 1} onClick={() => goPage(result.totalPages - 1)} aria-label="마지막 쪽">»</button>
                <span>{page + 1} / {result.totalPages.toLocaleString()}쪽</span>
              </nav>
            )}
          </section>

          <div className="stay-map-column"><SearchMap items={result.items} selectedId={selectedId} onSelect={setSelectedId} query={query} focus={focus} courseRoute={courseId ? courseRoute : null} /><p>별표는 현재 목록에 있는 숙소입니다.{courseId && ' 번호와 점선은 저장된 여행의 일차별 방문 순서입니다.'}</p></div>
        </div>
      </main>
    </div>
  );
}
