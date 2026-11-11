import './map-fallback.css';

export default function MapFallback({ onRetry, reason, width = '100%', height = '400px' }) {
  return <section className="map-fallback" style={{ width, height }} aria-label="지도를 불러오지 못했습니다">
    <div className="map-fallback__content">
      <div className="map-fallback__heading">
        <span className="map-fallback__icon" aria-hidden="true">⌁</span>
        <div><span className="map-fallback__eyebrow">LOCATION OVERVIEW</span><h2>지도를 불러오지 못했어요</h2></div>
      </div>
      <p className="map-fallback__description">지도 연결을 확인해 주세요. 아래 장소 목록은 계속 확인할 수 있습니다.</p>
      {reason && <p className="map-fallback__reason" role="alert">{reason}</p>}
      {onRetry && <button className="map-fallback__retry" type="button" onClick={onRetry}>다시 연결</button>}
    </div>
  </section>;
}
