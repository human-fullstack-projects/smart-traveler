import { useEffect, useMemo, useState } from 'react';
import KakaoMap from './KakaoMap';
import MapFallback from './MapFallback';
import MapPlaceCards from './MapPlaceCards';
import ItineraryPlaceList from './ItineraryPlaceList';
import { loadKakaoMaps } from './loadKakaoMaps';
import { hasValidCoordinates, resolveKakaoPlace } from './resolveKakaoPlace';
import './itinerary-map-panel.css';

const EMPTY_DAYS = [];
const EMPTY_PLACES = [];

function unavailable(place) {
  return { ...place, locationStatus: hasValidCoordinates(place) ? 'ready' : 'unresolved',
    addressStatus: place.address ? 'provided' : 'unavailable' };
}

/**
 * days: [{ day: 1, places: [{ id, name, address?, kind?: 'course'|'stay', latitude?, longitude? }] }]
 * Visit numbers restart each day. Lodging does not take a visit number.
 */
export default function ItineraryMapPanel({ days = EMPTY_DAYS, center, searchRegion = '', level = 5, height = '320px', extraPlaces = EMPTY_PLACES,
  extraTitle = '주변 숙소', previewFailure = false, sourceNote, onPlaceSelect, showIntro = true, showCategories = true, showPlaceList = true, allDaysLabel = '전체 코스', focusPlace, loadPlaceDetails, loadPlacePhotos, renderPlaceDetails, renderExtraPlaceDetails }) {
  const [category, setCategory] = useState('all');
  const [activeDay, setActiveDay] = useState('all');
  const [selectedId, setSelectedId] = useState(null);
  const [mapStatus, setMapStatus] = useState('loading');
  const [resolvedById, setResolvedById] = useState(() => new Map());

  useEffect(() => {
    if (!focusPlace?.id) return;
    setCategory('all');
    setActiveDay('all');
    setSelectedId(focusPlace.id);
  }, [focusPlace]);

  useEffect(() => {
    let cancelled = false;
    const places = [...days.flatMap((day) => day.places || []), ...extraPlaces];
    setResolvedById(new Map());
    if (!places.length) return () => { cancelled = true; };
    loadKakaoMaps().then(async (maps) => {
      if (!maps.services?.Geocoder || !maps.services?.Places) throw new Error('장소 검색 서비스를 사용할 수 없습니다.');
      return Promise.all(places.map(async (place) => {
        if (place.kind === 'stay' && hasValidCoordinates(place) && place.address) {
          return { ...place, locationStatus: 'ready', addressStatus: 'provided' };
        }
        try { return await resolveKakaoPlace(maps, place, searchRegion); }
        catch { return unavailable(place); }
      }));
    }).catch(() => places.map(unavailable)).then((resolved) => {
      if (!cancelled) setResolvedById(new Map(resolved.map((place) => [place.id, place])));
    });
    return () => { cancelled = true; };
  }, [days, extraPlaces, searchRegion]);

  const numberedDays = useMemo(() => {
    return days.map((day) => {
      let visit = 0;
      let stay = 0;
      return { ...day, places: (day.places || []).map((source) => {
        const place = resolvedById.get(source.id) || source;
        if (place.kind === 'stay') {
          stay += 1;
          const label = `${day.day}-숙소${stay > 1 ? stay : ''}`;
          return { ...place, kind: 'stay', markerLabel: label, badgeLabel: label };
        }
        visit += 1;
        const label = `${day.day}-${visit}`;
        return { ...place, kind: 'course', number: label, markerLabel: label, badgeLabel: label };
      }) };
    });
  }, [days, resolvedById]);
  const resolvedExtras = useMemo(() => extraPlaces.map((source) => ({
    ...(resolvedById.get(source.id) || source), kind: 'stay', markerLabel: source.markerLabel || '숙소', badgeLabel: '숙소',
  })), [extraPlaces, resolvedById]);
  const hasStays = numberedDays.some((day) => day.places.some((place) => place.kind === 'stay')) || resolvedExtras.length > 0;
  const visibleDays = useMemo(() => numberedDays
    .filter((day) => activeDay === 'all' || String(day.day) === String(activeDay))
    .map((day) => ({ ...day, places: day.places.filter((place) => category === 'all' || place.kind === category) })), [numberedDays, activeDay, category]);
  const visibleExtras = activeDay === 'all' && category !== 'course' ? resolvedExtras : EMPTY_PLACES;
  const visiblePlaces = useMemo(() => [...visibleDays.flatMap((day) => day.places), ...visibleExtras]
    .filter(hasValidCoordinates), [visibleDays, visibleExtras]);
  const paths = useMemo(() => category === 'stay' ? [] : visibleDays.map((day) => ({
    points: day.places.filter((place) => place.kind === 'course' && hasValidCoordinates(place)), dashed: true,
  })), [visibleDays, category]);
  const selectedPosition = visiblePlaces.find((place) => String(place.id) === String(selectedId));
  const selected = selectedPosition || visibleDays.flatMap((day) => day.places).find((place) => String(place.id) === String(selectedId));

  function selectPlace(place) {
    const isExtra = visibleExtras.some(extra => extra.id === place.id);
    const canCollapse = isExtra ? renderExtraPlaceDetails : loadPlaceDetails;
    setSelectedId(current => canCollapse && current === place.id ? null : place.id);
    onPlaceSelect?.(place);
  }

  return <div className="itinerary-map">
    {showIntro && <p className="itinerary-map__intro">{renderPlaceDetails ? '장소를 누르면 지도에서 위치를 확인하고 사진을 펼쳐 볼 수 있습니다.'
      : '장소를 누르면 지도와 마커가 해당 위치로 이동합니다. 지도가 열리지 않아도 일정 목록을 볼 수 있습니다.'}</p>}
    {showCategories && <div className="itinerary-map__categories" role="group" aria-label="장소 종류">
      {['all', 'course', ...(hasStays ? ['stay'] : [])].map((value) => <button key={value} type="button" aria-pressed={category === value}
        onClick={() => { setCategory(value); setSelectedId(null); }}>{{ all: '전체', course: '방문지', stay: '숙소' }[value]}</button>)}
    </div>}
    {numberedDays.length > 1 && <div className="itinerary-map__days" role="group" aria-label="여행 일자">
      <button type="button" aria-pressed={activeDay === 'all'} onClick={() => { setActiveDay('all'); setSelectedId(null); }}>{allDaysLabel}</button>
      {numberedDays.map((day) => <button key={day.day} type="button" aria-pressed={String(activeDay) === String(day.day)}
        onClick={() => { setActiveDay(day.day); setSelectedId(null); }}>{day.day}일차</button>)}
    </div>}
    <div className="itinerary-map__frame">{previewFailure
      ? <MapFallback reason="연결이 끊긴 상황을 가정한 미리보기입니다." height={height} />
      : <KakaoMap center={selectedPosition || center} places={visiblePlaces} paths={paths} selectedId={selectedId}
        fitPoints={selectedPosition ? undefined : visiblePlaces} level={level} height={height}
        onMarkerClick={selectPlace} onStatusChange={setMapStatus} />}
    </div>
    {sourceNote && <p className="itinerary-map__source">{sourceNote}</p>}
    {showPlaceList && <ItineraryPlaceList days={visibleDays} selectedId={selectedId} onSelect={selectPlace}
      loadDetails={loadPlaceDetails} loadPhotos={loadPlacePhotos} renderDetails={renderPlaceDetails} searchRegion={searchRegion} />}
    {visibleExtras.length > 0 && <section className="itinerary-map__day itinerary-map__extras">
      <h3>{extraTitle}</h3><MapPlaceCards places={visibleExtras} selectedId={selectedId} onSelect={selectPlace}
        renderDetails={renderExtraPlaceDetails} />
    </section>}
    {selected && !selectedPosition && mapStatus === 'ready' && !previewFailure && selected.locationStatus !== 'unresolved' && <p className="itinerary-map__notice" role="status">{selected.name}의 지도 위치를 확인하고 있습니다.</p>}
  </div>;
}
