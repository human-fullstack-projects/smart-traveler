import { useState } from 'react';
import KakaoMap from '../../components/map/KakaoMap';

const testPlaces = [
  { id: 1, name: '서울시청', latitude: 37.5665, longitude: 126.9780 },
];

export default function MapDemoPage() {
  const [selectedPlace, setSelectedPlace] = useState(null);

  return <section style={{ padding: '24px', maxWidth: '960px', margin: '0 auto' }}>
    <h1>카카오맵 테스트</h1>
    <KakaoMap places={testPlaces} level={3} height="400px" onMarkerClick={setSelectedPlace} />
    {selectedPlace && <p role="status">선택한 장소: {selectedPlace.name}</p>}
  </section>;
}
