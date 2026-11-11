import { useState } from 'react';
import ItineraryMapPanel from '../../components/map/ItineraryMapPanel';
import './map-demo.css';

const testDays = [
  { day: 1, places: [
    { id: 1, name: '서울시청', address: '서울 중구 세종대로 110', latitude: 37.5665, longitude: 126.9780 },
    { id: 2, name: '덕수궁', address: '서울특별시 중구 세종대로 99' },
    { id: 3, kind: 'stay', name: '시연용 숙소', latitude: 37.5683, longitude: 126.9797 },
    { id: 5, name: '확인 불가 시연 장소 ZZZ' },
  ] },
  { day: 2, places: [
    { id: 4, name: '광화문' },
  ] },
];

export default function MapDemoPage() {
  const [mode, setMode] = useState('map');

  return <section className="map-demo">
    <div className="map-demo__intro"><span>MAP COMPONENT PREVIEW</span><h1>코스 지도 미리보기</h1><p>방문지와 숙소를 날짜별로 확인하고, 연결 실패 시 표시되는 화면도 볼 수 있습니다.</p></div>
    <div className="map-demo__switch" role="group" aria-label="지도 상태 미리보기">
      <button type="button" aria-pressed={mode === 'map'} onClick={() => setMode('map')}>카카오맵</button>
      <button type="button" aria-pressed={mode === 'fallback'} onClick={() => setMode('fallback')}>연결 실패 화면 미리보기</button>
    </div>
    <ItineraryMapPanel days={testDays} center={testDays[0].places[0]} searchRegion="서울" level={4} height="400px"
      previewFailure={mode === 'fallback'} sourceNote="이 페이지의 장소와 숙소는 시연용 데이터입니다. 주소만 있는 장소, 좌표만 있는 숙소, 이름만 있는 방문지와 위치 확인 실패 예시를 포함합니다." />
  </section>;
}
