import { useMemo } from 'react';
import ItineraryMapPanel from '../../../components/map/ItineraryMapPanel';
import { stays } from '../../stays/shared/stayData';

const centers = {
  부산: { latitude: 35.1796, longitude: 129.0756 },
  강릉: { latitude: 37.7519, longitude: 128.8761 },
  서울: { latitude: 37.5665, longitude: 126.9780 },
  제주: { latitude: 33.4996, longitude: 126.5312 },
  경주: { latitude: 35.8562, longitude: 129.2247 },
};
const EMPTY_PLAN = [];

export default function CourseMap({ region, plan = EMPTY_PLAN, showDemoStays = true }) {
  const days = useMemo(() => plan.map((day) => ({
    day: day.day,
    places: [
      ...(day.stops || []).map((stop, index) => ({
        ...(typeof stop === 'string' ? { name: stop.replace(/^\d{1,2}:\d{2}\s*/, '').trim() } : stop),
        id: `stop-${day.day}-${index}`, kind: 'course',
      })),
      ...(day.stays || (day.lodging ? [day.lodging] : [])).map((stay, index) => ({
        ...(typeof stay === 'string' ? { name: stay } : stay),
        id: `stay-${day.day}-${index}`, kind: 'stay',
      })),
    ],
  })), [plan]);
  const regionStays = useMemo(() => showDemoStays ? stays.filter((stay) => stay.region === region && stay.demoCoordinate).map((stay) => ({
    ...stay.demoCoordinate,
    id: `region-stay-${stay.id}`,
    name: stay.name,
    address: stay.address,
    kind: 'stay',
  })) : [], [region, showDemoStays]);

  return <ItineraryMapPanel
    days={days}
    center={centers[region]}
    searchRegion={region}
    level={5}
    height="320px"
    extraPlaces={regionStays}
    extraTitle="같은 지역 숙소 시연 목록"
    sourceNote={`온라인 지도 배경: 카카오 지도. 주소와 장소명은 카카오 검색 결과에 따라 실제 일정 장소와 다를 수 있습니다. ${showDemoStays ? '같은 지역 숙소는 시연용 데이터입니다. ' : ''}점선은 방문지 사이의 직선 개요로, 실제 이동 경로가 아닙니다.`}
  />;
}
