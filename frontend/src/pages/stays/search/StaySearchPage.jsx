import { useEffect, useMemo, useState } from 'react';
import { StayArtwork, StayFooter, StayHeader, StatusPill } from '../shared/StayShell.jsx';
import { formatWon, getDefaultStayDates, stays, stayTypes, toDateInputValue } from '../shared/stayData.js';
import { getBookableStays, getRooms } from '../shared/stayApi.js';
import KakaoMap from '../../../components/map/KakaoMap.jsx';
import { loadKakaoMaps } from '../../../components/map/loadKakaoMaps.js';
import { resolveDestinationCenter } from '../../../components/map/resolveKakaoPlace.js';
import '../../../css/stay-search.css';
import './stay-kakao-map.css';

const amenityOptions = ['주차', '조식', '오션뷰', '취사', '와이파이'];

const regionCenters = {
  부산: { latitude: 35.1796, longitude: 129.0756 },
  서울: { latitude: 37.5665, longitude: 126.9780 },
  제주: { latitude: 33.4996, longitude: 126.5312 },
};

function matchDemoRegion(destination) {
  const text = String(destination || '');
  return Object.keys(regionCenters).find((region) => text.includes(region)) || '';
}

function readDestinationQuery() {
  return new URLSearchParams((window.location.hash.split('?')[1] || '').trim()).get('destination') || '';
}

function SearchMap({ results, selectedId, onSelect, query, region, destinationCenter }) {
  const [fitRequest, setFitRequest] = useState(0);
  const [mapStatus, setMapStatus] = useState('loading');
  const places = useMemo(() => results.filter((stay) => stay.demoCoordinate).map((stay) => ({
    ...stay.demoCoordinate,
    id: stay.id,
    name: stay.name,
    kind: 'stay',
    markerLabel: stay.saleable ? '예약' : '정보',
  })), [results]);
  const selected = results.find((stay) => stay.id === selectedId) || results[0];
  const center = selected?.demoCoordinate || destinationCenter || regionCenters[region];
  const bookingQuery = selected ? new URLSearchParams({ stay: selected.id, ...query }).toString() : '';

  return (
    <section className="stay-kakao-map" aria-label="숙소 검색 결과 지도">
      <KakaoMap places={places} center={center} selectedId={selected?.id} fitPoints={places} fitRequest={fitRequest} onMarkerClick={(place) => onSelect(place.id)} onStatusChange={setMapStatus} height="100%" />
      {mapStatus === 'ready' && <div className="stay-kakao-map__tools"><button type="button" onClick={() => setFitRequest((value) => value + 1)}>전체 보기</button><span>카카오 지도 · 숙소 위치</span></div>}
      {mapStatus === 'ready' && selected && <div className="stay-kakao-map__details" role="status"><strong>{selected.name}</strong><span>{selected.saleable ? `1박 ${formatWon(selected.price)}부터` : '판매 정보 미확인'}</span>{selected.saleable && <a href={`#/stays/booking?${bookingQuery}`}>객실 보기 →</a>}</div>}
    </section>
  );
}

function StayCard({ stay, selected, query, onSelect }) {
  const [photoFailed, setPhotoFailed] = useState(false);
  const bookingQuery = new URLSearchParams({ stay: stay.id, ...query }).toString();
  return (
    <article className={`stay-result-card${selected ? ' is-selected' : ''}`} onMouseEnter={() => onSelect(stay.id)}>
      <button type="button" className="stay-result-card__image" onClick={() => onSelect(stay.id)} aria-pressed={selected} aria-label={`${stay.name} 지도에서 보기`}>
        {stay.photoUrl && !photoFailed
          ? <img className="stay-result-card__photo" src={stay.photoUrl} alt={`${stay.name} 사진`} loading="lazy" referrerPolicy="no-referrer" onError={() => setPhotoFailed(true)} />
          : <StayArtwork theme={stay.theme} label={`${stay.name} 이미지 영역`} compact />}
        {stay.saleable && stay.remaining <= 3 && <span className="stay-result-card__remaining">남은 객실 {stay.remaining}</span>}
      </button>
      <div className="stay-result-card__body">
        <div className="stay-result-card__top">
          <div>
            <p className="stay-result-card__category">{stay.type} · {stay.area}</p>
            <h2>{stay.name}</h2>
          </div>
          {stay.rating != null && <span className="stay-result-card__rating">★ {stay.rating} <small>({stay.reviewCount})</small></span>}
        </div>
        <p className="stay-result-card__description">{stay.description}</p>
        <div className="stay-result-card__chips">
          {stay.amenities.slice(0, 4).map((item) => <span key={item}>{item}</span>)}
        </div>
        <div className="stay-result-card__bottom">
          <div>
            <StatusPill tone={stay.saleable ? 'success' : 'warning'}>{stay.saleable ? '내부 재고 확인' : '정보 조회 전용'}</StatusPill>
            <span className="stay-result-card__source">{stay.source}</span>
          </div>
          {stay.saleable ? (
            <div className="stay-result-card__price">
              <span>1박 평균 최저가</span><strong>{formatWon(stay.price)}</strong>
              <a href={`#/stays/booking?${bookingQuery}`}>객실 보기</a>
            </div>
          ) : (
            <div className="stay-result-card__price is-info"><span>판매 정보 미확인</span><strong>예약 불가</strong><button type="button" onClick={() => onSelect(stay.id)}>상세 정보</button></div>
          )}
        </div>
      </div>
    </article>
  );
}

export default function StaySearchPage() {
  const defaultDates = useMemo(() => getDefaultStayDates(), []);
  const today = useMemo(() => toDateInputValue(new Date()), []);
  const destinationQuery = useMemo(() => readDestinationQuery(), []);
  const initialRegion = useMemo(() => matchDemoRegion(destinationQuery) || '부산', [destinationQuery]);
  const [search, setSearch] = useState({ region: initialRegion, ...defaultDates, adults: 2, children: 0 });
  const [applied, setApplied] = useState(search);
  const [sort, setSort] = useState('distance');
  const [types, setTypes] = useState([]);
  const [amenities, setAmenities] = useState([]);
  const [maxPrice, setMaxPrice] = useState(250000);
  const [bookableOnly, setBookableOnly] = useState(false);
  const [selectedId, setSelectedId] = useState(stays[0].id);
  const [notice, setNotice] = useState('');
  const [liveStays, setLiveStays] = useState([]);
  const [stayError, setStayError] = useState('');
  const [destinationCenter, setDestinationCenter] = useState(null);

  useEffect(() => {
    if (!destinationQuery) return;
    let cancelled = false;
    loadKakaoMaps()
      .then((maps) => resolveDestinationCenter(maps, destinationQuery))
      .then((center) => { if (!cancelled && center) setDestinationCenter(center); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [destinationQuery]);

  useEffect(() => {
    if (destinationQuery && !matchDemoRegion(destinationQuery)) {
      setNotice(`'${destinationQuery}' 지역은 데모 숙소 데이터가 없어 목록은 기본 지역(${initialRegion}) 결과를 표시합니다. 지도는 해당 위치로 이동합니다.`);
    }
    // Only runs once on mount for the initial destination query.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let active = true;
    setLiveStays([]);
    setStayError('');
    getBookableStays()
      .then((data) => Promise.all(data.filter((stay) => stay.address.startsWith(applied.region)).map(async (stay) => {
        const rooms = await getRooms(stay.lodgingId, applied.checkIn, applied.checkOut, applied.adults + applied.children);
        const price = rooms.length
          ? Math.round(Math.min(...rooms.map((room) => room.totalPrice / room.nights.length)))
          : 0;
        return {
          id: String(stay.lodgingId), name: stay.name, region: applied.region,
          type: { HOTEL: '호텔', RESORT: '리조트', PENSION: '펜션', GUESTHOUSE: '게스트하우스' }[stay.lodgingType] || stay.lodgingType,
          area: stay.address, description: '', amenities: [], theme: 'ocean',
          price, saleable: rooms.length > 0, source: '숙소 정보',
          demoCoordinate: { latitude: stay.latitude, longitude: stay.longitude },
          photoUrl: stay.thumbnailUrl || null,
        };
      })))
      .then((data) => { if (active) setLiveStays(data); })
      .catch((error) => { if (active) setStayError(error.message); });
    return () => { active = false; };
  }, [applied]);

  const results = useMemo(() => {
    const infoStays = stays.map((stay) => ({ ...stay, saleable: false }));
    const filtered = [...liveStays, ...infoStays].filter((stay) => {
      if (stay.region !== applied.region) return false;
      if (types.length && !types.includes(stay.type)) return false;
      if (amenities.length && !amenities.every((item) => stay.amenities.includes(item))) return false;
      if (stay.saleable && stay.price > maxPrice) return false;
      if (bookableOnly && !stay.saleable) return false;
      return true;
    });
    return [...filtered].sort((a, b) => {
      if (sort === 'price') return (a.saleable ? a.price : Infinity) - (b.saleable ? b.price : Infinity);
      if (sort === 'rating') return (b.rating || 0) - (a.rating || 0);
      return Number(b.saleable) - Number(a.saleable) || (a.distanceKm || 0) - (b.distanceKm || 0);
    });
  }, [amenities, applied.region, bookableOnly, liveStays, maxPrice, sort, types]);

  function toggleFilter(value, setter) {
    setter((current) => current.includes(value) ? current.filter((item) => item !== value) : [...current, value]);
  }

  function submit(event) {
    event.preventDefault();
    if (!search.checkIn || !search.checkOut || search.checkOut <= search.checkIn) {
      setNotice('체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.');
      return;
    }
    setApplied(search);
    setNotice('');
  }

  const query = {
    checkIn: applied.checkIn,
    checkOut: applied.checkOut,
    adults: String(applied.adults),
    children: String(applied.children),
  };

  return (
    <div className="stay-ui stay-search-page">
      <StayHeader active="search" />
      <main className="stay-search-main">
        <section className="stay-search-hero">
          <div><span>STAY FINDER</span><h1>여행에 맞는 숙소를 찾아보세요.</h1><p>선택한 날짜 전체에 판매 가능한 객실만 예약 단계에서 다시 확인합니다.</p></div>
          <StatusPill tone="info">목록 · 지도 함께 보기</StatusPill>
        </section>

        <form className="stay-search-bar" onSubmit={submit}>
          <label><span>지역</span><select value={search.region} onChange={(event) => setSearch({ ...search, region: event.target.value })}><option>부산</option><option>서울</option><option>제주</option></select></label>
          <label><span>체크인</span><input type="date" min={today} value={search.checkIn} onChange={(event) => setSearch({ ...search, checkIn: event.target.value })} /></label>
          <label><span>체크아웃</span><input type="date" min={search.checkIn || today} value={search.checkOut} onChange={(event) => setSearch({ ...search, checkOut: event.target.value })} /></label>
          <label><span>성인</span><input type="number" min="1" max="10" value={search.adults} onChange={(event) => setSearch({ ...search, adults: Number(event.target.value) })} /></label>
          <label><span>아동</span><input type="number" min="0" max="8" value={search.children} onChange={(event) => setSearch({ ...search, children: Number(event.target.value) })} /></label>
          <button type="submit">숙소 검색</button>
        </form>
        {notice && <p className="stay-search-notice" role="status">{notice}</p>}
        {stayError && <p className="stay-search-notice" role="alert">{stayError}</p>}

        <div className="stay-search-layout">
          <aside className="stay-filter" aria-label="숙소 검색 필터">
            <div className="stay-filter__head"><h2>필터</h2><button type="button" onClick={() => { setTypes([]); setAmenities([]); setMaxPrice(250000); setBookableOnly(false); }}>초기화</button></div>
            <fieldset><legend>숙소 유형</legend>{stayTypes.map((type) => <label key={type}><input type="checkbox" checked={types.includes(type)} onChange={() => toggleFilter(type, setTypes)} />{type}</label>)}</fieldset>
            <fieldset><legend>편의시설</legend>{amenityOptions.map((item) => <label key={item}><input type="checkbox" checked={amenities.includes(item)} onChange={() => toggleFilter(item, setAmenities)} />{item}</label>)}</fieldset>
            <fieldset><legend>1박 최대 요금</legend><input className="stay-filter__range" type="range" min="80000" max="300000" step="10000" value={maxPrice} onChange={(event) => setMaxPrice(Number(event.target.value))} /><strong>{formatWon(maxPrice)} 이하</strong></fieldset>
            <label className="stay-filter__switch"><input type="checkbox" checked={bookableOnly} onChange={(event) => setBookableOnly(event.target.checked)} /><span />예약 가능 숙소만</label>
          </aside>

          <section className="stay-results" aria-label="숙소 검색 결과">
            <div className="stay-results__toolbar"><p><strong>{applied.region}</strong> 검색 결과 <b>{results.length}</b>곳</p><select aria-label="정렬 기준" value={sort} onChange={(event) => setSort(event.target.value)}><option value="distance">가까운 순</option><option value="price">낮은 가격 순</option><option value="rating">평점 높은 순</option></select></div>
            {results.length ? results.map((stay) => <StayCard key={stay.id} stay={stay} selected={selectedId === stay.id} query={query} onSelect={setSelectedId} />) : <div className="stay-results__empty"><strong>조건에 맞는 숙소가 없습니다.</strong><span>가격 범위를 넓히거나 다른 지역·날짜를 선택해 주세요.</span></div>}
          </section>

          <div className="stay-map-column"><SearchMap results={results} selectedId={selectedId} onSelect={setSelectedId} query={query} region={applied.region} destinationCenter={destinationCenter} /><p>검색 필터와 지도 마커는 같은 숙소 목록을 사용합니다. 정보 조회 전용 숙소의 좌표는 시연용입니다.</p></div>
        </div>
      </main>
      <StayFooter />
    </div>
  );
}
