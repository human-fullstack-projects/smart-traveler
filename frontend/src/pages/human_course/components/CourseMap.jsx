import { useEffect, useMemo, useState } from 'react';
import KakaoMap from '../../../components/map/KakaoMap';
import { loadKakaoMaps } from '../../../components/map/loadKakaoMaps';
import { formatWon, stays } from '../../stays/shared/stayData';

const centers = {
  부산: { latitude: 35.1796, longitude: 129.0756 },
  강릉: { latitude: 37.7519, longitude: 128.8761 },
  서울: { latitude: 37.5665, longitude: 126.9780 },
  제주: { latitude: 33.4996, longitude: 126.5312 },
  경주: { latitude: 35.8562, longitude: 129.2247 },
};
const EMPTY_PLAN = [];

function findPlace(search, maps, query) {
  return new Promise((resolve) => {
    search.keywordSearch(query, (results, status) => {
      resolve(status === maps.services.Status.OK && results?.length ? results[0] : null);
    });
  });
}

export default function CourseMap({ region, plan = EMPTY_PLAN }) {
  const stops = useMemo(() => plan.flatMap((day) => day.stops.map((stop, index) => ({
    id: `${day.day}-${index}`,
    day: day.day,
    number: index + 1,
    name: stop.replace(/^\d{1,2}:\d{2}\s*/, '').trim(),
  }))), [plan]);
  const [coursePlaces, setCoursePlaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [view, setView] = useState('course');
  const [selectedId, setSelectedId] = useState(null);
  const [fitRequest, setFitRequest] = useState(0);
  const regionCenter = centers[region];
  const stayResults = useMemo(() => stays.filter((stay) => stay.region === region && stay.demoCoordinate), [region]);
  const stayPlaces = useMemo(() => stayResults.map((stay) => ({
    ...stay.demoCoordinate,
    id: stay.id,
    name: stay.name,
    kind: 'stay',
    markerLabel: stay.saleable ? `${Math.round(stay.price / 10000)}만` : '정보',
  })), [stayResults]);
  const visiblePlaces = useMemo(() => [
    ...(view === 'stay' ? [] : coursePlaces),
    ...(view === 'course' ? [] : stayPlaces),
  ], [coursePlaces, stayPlaces, view]);
  const paths = useMemo(() => view === 'stay' ? [] : plan.map((day) => ({
    points: coursePlaces.filter((place) => place.day === day.day),
    dashed: true,
  })), [coursePlaces, plan, view]);
  const selected = visiblePlaces.find((place) => place.id === selectedId);
  const center = selected || regionCenter;
  const selectedStay = stayResults.find((stay) => stay.id === selectedId);

  useEffect(() => {
    let cancelled = false;
    setCoursePlaces([]);
    setLoading(true);
    setMessage('방문지 위치를 카카오 장소 검색으로 확인하고 있습니다.');
    loadKakaoMaps().then(async (maps) => {
      if (!maps.services?.Places) throw new Error('카카오 장소 검색 서비스를 불러오지 못했습니다.');
      const results = await Promise.all(stops.map(async (stop) => {
        const match = await findPlace(new maps.services.Places(), maps, `${region} ${stop.name}`);
        if (!match) return null;
        return { ...stop, name: match.place_name || stop.name, address: match.address_name,
          latitude: Number(match.y), longitude: Number(match.x), markerLabel: String(stop.number), kind: 'course' };
      }));
      if (cancelled) return;
      const found = results.filter(Boolean);
      setCoursePlaces(found);
      setMessage(found.length < stops.length ? `${stops.length - found.length}개 방문지는 위치를 찾지 못해 지도에 표시하지 않았습니다.` : '');
    }).catch((error) => { if (!cancelled) setMessage(error.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [region, stops]);

  if (!regionCenter) return <p className="note">이 지역의 지도 미리보기가 아직 준비되지 않았습니다.</p>;

  function selectStop(id) {
    if (!loading && !coursePlaces.some((place) => place.id === id)) return;
    setView('course');
    setSelectedId(id);
  }

  return <div className="course-map">
    <p className="form-help">방문지를 누르면 지도와 번호 마커가 이동합니다. 마커를 눌러도 같은 방문지가 선택됩니다.</p>
    <div className="map-controls" role="group" aria-label="지도 표시 대상">
      {['all', 'course', 'stay'].map((mode) => <button key={mode} type="button" aria-pressed={view === mode} onClick={() => { setView(mode); setSelectedId(null); }}>
        {{ all: '전체', course: '코스', stay: '숙소' }[mode]}
      </button>)}
      <button type="button" onClick={() => { setSelectedId(null); setFitRequest((value) => value + 1); }}>전체 위치 보기</button>
    </div>
    <div className="map-frame"><KakaoMap center={center} places={visiblePlaces} paths={paths} selectedId={selectedId} fitPoints={selectedId ? undefined : visiblePlaces} fitRequest={fitRequest} level={5} height="320px" onMarkerClick={(place) => setSelectedId(place.id)} />
      {selected && <div className="map-selected-place" role="status"><strong>{selected.name}</strong>
        <span>{selected.kind === 'stay' ? `${selectedStay?.saleable ? `${formatWon(selectedStay.price)}부터` : '판매 정보 미확인'} · 시연용 좌표` : `${selected.address || '주소 미확인'} · 카카오 장소 검색 결과`}</span>
        {selectedStay?.saleable && <a href={`#/stays/booking?stay=${encodeURIComponent(selectedStay.id)}`}>객실 보기 →</a>}
      </div>}
    </div>
    {message && <p className="map-search-message" role="status">{message}</p>}
    <p className="map-source">지도 배경: 카카오 지도. 방문지 좌표: 카카오 장소 검색 결과이며 실제 일정 장소와 다를 수 있습니다. 숙소 좌표·요금: 시연용 데이터. 점선은 방문지 사이의 직선 개요로, 실제 이동 경로가 아닙니다.</p>
    {view !== 'stay' && <div className="map-stop-days">{plan.map((day) => <section className="map-stop-day" key={day.day}>
      <h3>{day.day}일차</h3>
      <ul className="map-stops">{day.stops.map((stop, index) => {
        const id = `${day.day}-${index}`;
        const place = stop.replace(/^\d{1,2}:\d{2}\s*/, '').trim();
        return <li key={id}><button type="button" onClick={() => selectStop(id)} disabled={!loading && !coursePlaces.some((item) => item.id === id)} aria-pressed={selectedId === id}>{index + 1}. {place}</button></li>;
      })}</ul>
    </section>)}</div>}
    {view !== 'course' && <section className="map-stay-results"><h3>같은 지역 숙소 시연 목록</h3>
      {stayResults.length ? <ul>{stayResults.map((stay) => <li key={stay.id}><button type="button" onClick={() => setSelectedId(stay.id)} aria-pressed={selectedId === stay.id}>{stay.name} · {stay.saleable ? `${formatWon(stay.price)}부터` : '정보 조회'}</button></li>)}</ul> : <p>이 지역의 숙소 시연 데이터가 없습니다.</p>}
    </section>}
  </div>;
}
