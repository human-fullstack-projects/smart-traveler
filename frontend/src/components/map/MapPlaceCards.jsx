import './map-place-cards.css';

export default function MapPlaceCards({ places, selectedId, onSelect }) {
  return <div className="map-place-cards">{places.map((place) => <button
    key={place.id}
    type="button"
    className={`map-place-card${place.locationStatus === 'unresolved' ? ' is-unresolved' : ''}`}
    aria-pressed={String(selectedId) === String(place.id)}
    onClick={() => onSelect(place)}
  >
    <span className={`map-place-card__badge${place.kind === 'stay' ? ' is-stay' : ''}`}>{place.badgeLabel || (place.kind === 'stay' ? '숙소' : place.number)}</span>
    <span className="map-place-card__text"><strong>{place.name}</strong>
      <small>{place.locationStatus === 'unresolved' ? '주소 확인 불가' : place.address || (place.addressStatus === 'unavailable' ? '주소 확인 불가' : '주소 확인 중…')}</small>
      {place.locationStatus === 'unresolved' && place.address && <small>입력 주소: {place.address}</small>}
      {place.locationStatus === 'unresolved' && <small className="map-place-card__warning">지도 위치 확인 불가 · 마커 미표시</small>}
    </span>
    <span className="map-place-card__arrow" aria-hidden="true">{place.locationStatus === 'unresolved' ? '!' : '→'}</span>
  </button>)}</div>;
}
