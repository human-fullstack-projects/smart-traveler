# 카카오맵 컴포넌트 사용법

## 날짜별 코스 지도

페이지에서 `ItineraryMapPanel`을 불러와 `days`를 전달합니다. 아래 import 경로는 `frontend/src/pages/MapExample.jsx` 기준입니다.

```jsx
import ItineraryMapPanel from '../components/map/ItineraryMapPanel';

const days = [
  {
    day: 1,
    places: [
      { id: 'city-hall', name: '서울시청', address: '서울 중구 세종대로 110' },
      { id: 'hotel', kind: 'stay', name: '시연용 숙소', latitude: 37.5683, longitude: 126.9797 },
    ],
  },
  { day: 2, places: [{ id: 'gwanghwamun', name: '광화문' }] },
];

export default function MapExample() {
  return <ItineraryMapPanel
    days={days}
    center={{ latitude: 37.5665, longitude: 126.9780 }}
    searchRegion="서울"
    level={4}
    height="400px"
    onPlaceSelect={(place) => console.log(place.name)}
  />;
}
```

- `days`: `{ day, places }[]`. 각 장소에는 **서로 다른 `id`와 `name`**을 넣습니다. `kind: 'stay'`가 없으면 방문지로 취급합니다. 방문지 번호(`1-1`, `1-2`, `2-1`)와 숙소 표시(`1-숙소`)는 자동으로 붙습니다.
- 장소의 `address`, `latitude`, `longitude`는 가능한 만큼 전달합니다. 좌표가 없으면 주소 → 이름 순서로 검색하고, 좌표만 있으면 주소를 조회합니다. 이름 검색은 해당 지역에서 정확히 일치하는 장소가 한 곳일 때 사용합니다. 위치를 확인하지 못하면 목록에 안내를 표시하고 마커는 생략합니다.
- `searchRegion`: 이름만 있는 장소를 검색할 때 사용할 지역명입니다. 동명이인 장소를 줄이려면 전달하는 것이 좋습니다.
- `center`: 기본 지도 중심 `{ latitude, longitude }`. 마커가 있으면 보이는 마커에 맞춰 지도가 조정됩니다. `level`은 확대 수준(기본 `5`), `height`는 지도 높이(기본 `320px`)입니다.
- `onPlaceSelect`: 장소 카드 또는 마커를 누르면 선택된 장소 객체를 받습니다. 필요 없으면 생략합니다.
- 선택 사항: `extraPlaces`로 날짜에 속하지 않는 숙소 목록을 추가하고 `extraTitle`로 그 목록 제목을 정할 수 있습니다. `sourceNote`는 지도 아래 안내 문구입니다.

Spring Boot 응답이 위 `days` 형식이면 그대로 `days={response.days}`로 전달하면 됩니다. 응답 필드명이 다르면 페이지에서 이 형식으로 변환해 전달하세요.

## 지도와 마커만 필요한 경우

```jsx
import KakaoMap from '../components/map/KakaoMap';

<KakaoMap
  places={[{ id: 1, name: '서울시청', latitude: 37.5665, longitude: 126.9780 }]}
  center={{ latitude: 37.5665, longitude: 126.9780 }}
  level={3}
  height="400px"
  onMarkerClick={(place) => console.log(place.name)}
/>
```

`KakaoMap`에 직접 전달하는 `places`에는 **좌표가 필요**합니다. 주소·이름 검색까지 필요하면 위의 `ItineraryMapPanel`을 사용하세요.

지도 SDK 키는 `frontend/.env.local`의 `VITE_KAKAO_MAP_APP_KEY`에 설정합니다. 설정 예시는 `frontend/.env.example`에 있습니다.
