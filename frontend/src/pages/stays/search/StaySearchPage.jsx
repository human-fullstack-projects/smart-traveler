import { useEffect, useMemo, useRef, useState } from 'react';
import { StayArtwork, StatusPill } from '../shared/StayShell.jsx';
import { formatWon, getDefaultStayDates, stayTypeLabels, toDateInputValue } from '../shared/stayData.js';
import { getStayMap, getStayRegions, searchStays } from '../shared/stayApi.js';
import KakaoMap from '../../../components/map/KakaoMap.jsx';
import { loadKakaoMaps } from '../../../components/map/loadKakaoMaps.js';
import { resolveDestinationCenter } from '../../../components/map/resolveKakaoPlace.js';
import '../../../css/stay-search.css';
import './stay-kakao-map.css';
import './stay-search-paging.css';

// DB lodging.amenities에 실제로 있는 값 중 많이 쓰는 것
const AMENITY_OPTIONS = ['주차', '와이파이', '취사', '바비큐', '조식', '수영장', '엘리베이터', '넷플릭스'];
const PRICE_MIN = 40000;
const PRICE_MAX = 300000; // 끝까지 올리면 「제한 없음」
const PAGE_SIZES = [5, 10];

function readDestinationQuery() {
  return new URLSearchParams((window.location.hash.split('?')[1] || '').trim()).get('destination') || '';
}

/** #/stays?sido=..&sigungu=.. 로 지역 ID가 직접 왔는지 읽는다(이름이 비슷한 지역을 구분하기 위해 ID를 우선 쓴다). */
function readRegionIdQuery() {
  const params = new URLSearchParams((window.location.hash.split('?')[1] || '').trim());
  return { sido: params.get('sido') || '', sigungu: params.get('sigungu') || '' };
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

function SearchMap({ points, items, bounds, selectedId, onSelect, query, focus, fitRequest, onIdle, loading }) {
  const [mapStatus, setMapStatus] = useState('loading');
  const places = useMemo(() => points.map((point) => point.count > 1
    ? { id: `cluster-${point.latitude}-${point.longitude}`, latitude: point.latitude, longitude: point.longitude, name: `숙소 ${point.count}곳`, kind: 'cluster', markerLabel: point.count.toLocaleString(), cluster: point }
    : { id: String(point.lodgingId), latitude: point.latitude, longitude: point.longitude, name: point.name, kind: 'stay', markerLabel: items.some((item) => String(item.lodgingId) === String(point.lodgingId)) ? '★' : '·' }), [points, items]);
  // 새 검색이면 조건에 맞는 숙소 전체 범위로 맞춤(멀리서 보면 숫자 묶음) → 확대하면 낱개 핀·★(지금 목록)
  // 페이지를 넘길 때 bounds가 새 배열로 와도 지도의 현재 확대 위치는 유지한다.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const fitPoints = useMemo(() => bounds ? [{ latitude: bounds[0], longitude: bounds[1] }, { latitude: bounds[2], longitude: bounds[3] }] : [], [fitRequest]);
  const selectedItem = items.find((item) => String(item.lodgingId) === String(selectedId));
  const selectedPoint = points.find((point) => point.count === 1 && String(point.lodgingId) === String(selectedId));
  const selectedName = selectedItem?.name || selectedPoint?.name;
  const bookingQuery = selectedId ? new URLSearchParams({ stay: selectedId, ...query }).toString() : '';

  function handleMarker(place) {
    if (place.kind === 'cluster') onSelect(null, { latitude: place.latitude, longitude: place.longitude });
    else onSelect(place.id);
  }

  return (
    <section className="stay-kakao-map" aria-label="숙소 검색 결과 지도">
      <KakaoMap places={places} level={9} selectedId={selectedId} fitPoints={fitPoints} fitRequest={fitRequest} focus={focus} onIdle={onIdle} onMarkerClick={handleMarker} onStatusChange={setMapStatus} height="100%" />
      {mapStatus === 'ready' && <div className="stay-kakao-map__tools"><span>{loading ? '지도 숙소 불러오는 중…' : '숫자 = 그 근처 숙소 수 · 누르면 확대 · ★ = 지금 목록'}</span></div>}
      {mapStatus === 'ready' && selectedName && <div className="stay-kakao-map__details" role="status"><strong>{selectedName}</strong><span>{selectedItem ? (selectedItem.saleable ? `1박 ${formatWon(selectedItem.minPrice)}부터` : '선택 날짜 판매 없음') : '지도에서 선택한 숙소'}</span><a href={`#/stays/booking?${bookingQuery}`}>객실 보기 →</a></div>}
    </section>
  );
}

function StayCard({ stay, selected, query, onSelect, onShow }) {
  const [photoFailed, setPhotoFailed] = useState(false);
  const id = String(stay.lodgingId);
  const bookingQuery = new URLSearchParams({ stay: id, ...query }).toString();
  return (
    <article className={`stay-result-card${selected ? ' is-selected' : ''}`} onMouseEnter={() => onSelect(id)}>
      <button type="button" className="stay-result-card__image" onClick={() => onShow(stay)} aria-pressed={selected} aria-label={`${stay.name} 지도에서 보기`}>
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
            <div className="stay-result-card__price is-info"><span>날짜·인원을 바꿔 보세요</span><strong>예약 불가</strong><button type="button" onClick={() => onShow(stay)}>지도에서 보기</button></div>
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
  const [regions, setRegions] = useState([]);
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
  const [result, setResult] = useState({ items: [], total: 0, totalPages: 1, bounds: null });
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState('');
  const [stayError, setStayError] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [destinationCenter, setDestinationCenter] = useState(null);
  const [view, setView] = useState(null);
  const [points, setPoints] = useState([]);
  const [mapLoading, setMapLoading] = useState(false);
  const [focus, setFocus] = useState(null);
  const [fitRequest, setFitRequest] = useState(0);
  const resultsRef = useRef(null);
  const lastCriteriaKey = useRef(null);

  // 지역 목록(시·도 18 + 시·군·구) → 목적지 글자가 있으면 그 지역으로 시작, 없으면 부산
  useEffect(() => {
    getStayRegions().then((data) => {
      setRegions(data);
      const byId = readRegionIdQuery();
      const sidoById = byId.sido ? data.find((item) => String(item.regionId) === byId.sido) : null;
      const matched = sidoById
        ? { sido: byId.sido, sigungu: sidoById.children.some((item) => String(item.regionId) === byId.sigungu) ? byId.sigungu : '' }
        : matchRegion(data, destinationQuery);
      if (destinationQuery && !byId.sido && !matched.sido) setNotice(`'${destinationQuery}' 지역을 찾지 못해 전국 결과를 보여 드려요.`);
      const busan = data.find((item) => item.name.startsWith('부산'));
      const start = { ...search, sido: matched.sido || (destinationQuery || byId.sido ? '' : String(busan?.regionId || '')), sigungu: matched.sigungu };
      setSearch(start);
      setApplied(start);
    }).catch((error) => setStayError(error.message));
    // 처음 한 번만
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!destinationQuery) return;
    let cancelled = false;
    loadKakaoMaps().then((maps) => resolveDestinationCenter(maps, destinationQuery))
      .then((center) => { if (!cancelled && center) setDestinationCenter(center); }).catch(() => {});
    return () => { cancelled = true; };
  }, [destinationQuery]);

  // 가격 막대는 손을 뗀 뒤 0.4초 있다가 적용(움직이는 동안 요청을 줄임)
  useEffect(() => {
    const timer = setTimeout(() => setAppliedMaxPrice(maxPrice), 400);
    return () => clearTimeout(timer);
  }, [maxPrice]);

  const conditions = useMemo(() => applied && ({
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
  }), [applied, types, amenities, appliedKeyword, appliedMaxPrice, bookableOnly]);

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
    const center = destinationCenter ? { lat: destinationCenter.latitude, lng: destinationCenter.longitude } : {};
    searchStays({ ...conditions, sort, page, size, ...center })
      .then((data) => { if (active) { setResult(data); setSelectedId(data.items[0] ? String(data.items[0].lodgingId) : null); } })
      .catch((error) => { if (active) setStayError(error.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [conditions, sort, page, size, destinationCenter, criteriaKey]);

  // 새 조건으로 첫 쪽이 나오면 지도를 그 숙소들에 맞춤
  useEffect(() => { if (page === 0 && result.page === 0) setFitRequest((value) => value + 1); }, [result, page]);

  // 지도: 보이는 범위 안 숙소만(많으면 묶음) — 움직임이 멈추고 0.3초 뒤 요청
  useEffect(() => {
    if (!conditions || !view) return undefined;
    let active = true;
    const timer = setTimeout(() => {
      setMapLoading(true);
      getStayMap({ ...conditions, swLat: view.swLat, swLng: view.swLng, neLat: view.neLat, neLng: view.neLng })
        .then((data) => { if (active) setPoints(data.points); })
        .catch(() => { if (active) setPoints([]); })
        .finally(() => { if (active) setMapLoading(false); });
    }, 300);
    return () => { active = false; clearTimeout(timer); };
  }, [conditions, view]);

  function toggleFilter(value, setter) {
    setter((current) => current.includes(value) ? current.filter((item) => item !== value) : [...current, value]);
  }

  function submit(event) {
    event.preventDefault();
    if (!search.checkIn || !search.checkOut || search.checkOut <= search.checkIn) {
      setNotice('체크아웃 날짜는 체크인 날짜보다 뒤여야 합니다.');
      return;
    }
    setApplied({ ...search });
    setAppliedKeyword(keyword);
    setNotice('');
  }

  function selectFromMap(id, clusterCenter) {
    if (clusterCenter) { setFocus((current) => ({ ...clusterCenter, level: Math.max(3, (view?.level || 9) - 2), key: (current?.key || 0) + 1 })); return; }
    setSelectedId(id);
  }

  function showOnMap(stay) {
    setSelectedId(String(stay.lodgingId));
    if (stay.latitude != null) setFocus((current) => ({ latitude: Number(stay.latitude), longitude: Number(stay.longitude), level: 4, key: (current?.key || 0) + 1 }));
  }

  function goPage(next) {
    setPage(next);
    resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  const sido = regions.find((item) => String(item.regionId) === String(search.sido));
  const appliedSido = applied && regions.find((item) => String(item.regionId) === String(applied.sido));
  const appliedSigungu = appliedSido?.children.find((item) => String(item.regionId) === String(applied.sigungu));
  const regionLabel = appliedSigungu ? `${appliedSido.name} ${appliedSigungu.name}` : appliedSido?.name || '전국';
  const query = applied ? { checkIn: applied.checkIn, checkOut: applied.checkOut, adults: String(applied.adults), children: String(applied.children) } : {};

  return (
    <div className="stay-ui stay-search-page">
      <main className="stay-search-main">
        <section className="stay-search-hero">
          <div><span>Stay Finder</span><h1>여행에 맞는 숙소를 찾아보세요.</h1><p>전국 숙소 중 조건에 맞는 곳을 {size}곳씩 보여 드려요. 선택한 날짜에 판매 중인 객실만 예약할 수 있어요.</p></div>
        </section>

        <form className="stay-search-bar stay-search-bar--regions" onSubmit={submit}>
          <label><span>시·도</span><select value={search.sido} onChange={(event) => setSearch({ ...search, sido: event.target.value, sigungu: '' })}><option value="">전국</option>{regions.map((item) => <option key={item.regionId} value={item.regionId}>{item.name} ({item.count.toLocaleString()})</option>)}</select></label>
          <label><span>시·군·구</span><select value={search.sigungu} disabled={!sido?.children.length} onChange={(event) => setSearch({ ...search, sigungu: event.target.value })}><option value="">전체</option>{(sido?.children || []).map((item) => <option key={item.regionId} value={item.regionId}>{item.name} ({item.count.toLocaleString()})</option>)}</select></label>
          <label><span>체크인</span><input type="date" min={today} value={search.checkIn} onChange={(event) => setSearch({ ...search, checkIn: event.target.value })} /></label>
          <label><span>체크아웃</span><input type="date" min={search.checkIn || today} value={search.checkOut} onChange={(event) => setSearch({ ...search, checkOut: event.target.value })} /></label>
          <label><span>성인</span><input type="number" min="1" max="10" value={search.adults} onChange={(event) => setSearch({ ...search, adults: Math.max(1, Number(event.target.value) || 1) })} /></label>
          <label><span>아동</span><input type="number" min="0" max="8" value={search.children} onChange={(event) => setSearch({ ...search, children: Math.max(0, Number(event.target.value) || 0) })} /></label>
          <button type="submit">숙소 검색</button>
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
              ? result.items.map((stay) => <StayCard key={stay.lodgingId} stay={stay} selected={selectedId === String(stay.lodgingId)} query={query} onSelect={setSelectedId} onShow={showOnMap} />)
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

          <div className="stay-map-column"><SearchMap points={points} items={result.items} bounds={result.bounds} selectedId={selectedId} onSelect={selectFromMap} query={query} focus={focus} fitRequest={fitRequest} onIdle={setView} loading={mapLoading} /><p>지도는 지금 보이는 범위의 숙소를 같은 조건으로 보여 줍니다. 멀리서 보면 근처 숙소를 숫자로 묶어 보여 줍니다.</p></div>
        </div>
      </main>
    </div>
  );
}
