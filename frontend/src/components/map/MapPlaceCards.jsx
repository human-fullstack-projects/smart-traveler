import { useEffect, useId, useRef } from 'react';
import MapPlaceDetails from './MapPlaceDetails';
import './map-place-cards.css';

export default function MapPlaceCards({ places, selectedId, onSelect, loadDetails, loadPhotos, renderDetails, searchRegion }) {
  const prefix = useId();
  const expandedRef = useRef(null);
  useEffect(() => {
    if (loadDetails) expandedRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [selectedId, loadDetails]);
  const hasDetails = Boolean(loadDetails || renderDetails);
  return <div className="map-place-cards">{places.map((place) => {
    const selected = String(selectedId) === String(place.id);
    const expanded = hasDetails && selected;
    const detailId = `${prefix}-${place.id}-details`;
    return <div key={place.id} className="map-place-item" ref={expanded ? expandedRef : undefined}><button
    type="button"
    className={`map-place-card${place.locationStatus === 'unresolved' ? ' is-unresolved' : ''}`}
    aria-pressed={selected}
    aria-expanded={hasDetails ? expanded : undefined}
    aria-controls={expanded ? detailId : undefined}
    onClick={() => onSelect(place)}
  >
    <span className={`map-place-card__badge${place.kind === 'stay' ? ' is-stay' : ''}`}>{place.badgeLabel || (place.kind === 'stay' ? '숙소' : place.number)}</span>
    <span className="map-place-card__text"><strong>{place.name}</strong>
      <small>{place.locationStatus === 'unresolved' ? '주소 확인 불가' : place.address || (place.addressStatus === 'unavailable' ? '주소 확인 불가' : '주소 확인 중…')}</small>
      {place.locationStatus === 'unresolved' && place.address && <small>입력 주소: {place.address}</small>}
      {place.locationStatus === 'unresolved' && <small className="map-place-card__warning">지도 위치 확인 불가 · 마커 미표시</small>}
    </span>
    <span className="map-place-card__arrow" aria-hidden="true">{renderDetails ? (expanded ? '▾' : '▸') : loadDetails ? (expanded ? '−' : '+') : place.locationStatus === 'unresolved' ? '!' : '→'}</span>
  </button>{expanded && <div id={detailId} className="map-place-detail" role="region" aria-label={`${place.name} 상세 정보`}>
    {renderDetails ? renderDetails(place) : <MapPlaceDetails place={place} region={searchRegion} loadDetails={loadDetails} loadPhotos={loadPhotos} />}
  </div>}</div>;
  })}</div>;
}
