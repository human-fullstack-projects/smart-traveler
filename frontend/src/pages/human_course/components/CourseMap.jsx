import { useMemo } from 'react';
import ItineraryMapPanel from '../../../components/map/ItineraryMapPanel';
import { PlacePhoto } from './CoursePlan';

const centers = {
  부산: { latitude: 35.1796, longitude: 129.0756 },
  강릉: { latitude: 37.7519, longitude: 128.8761 },
  서울: { latitude: 37.5665, longitude: 126.9780 },
  제주: { latitude: 33.4996, longitude: 126.5312 },
  경주: { latitude: 35.8562, longitude: 129.2247 },
};
const EMPTY_PLAN = [];

function renderPlacePhoto(place) {
  return <PlacePhoto key={place.id} name={place.name} imageUrls={place.imageUrls || []} emptyMessage="등록된 장소 사진이 없습니다." />;
}

export default function CourseMap({ region, plan = EMPTY_PLAN, stay, travelDates, preview = false, allDaysLabel }) {
  const days = useMemo(() => {
    const [checkIn, checkOut] = (travelDates || '').split('~').map((value) => Date.parse(value.trim()));
    return plan.map((day) => {
      const visits = [];
      const lodgings = [];
      const stops = day.places?.length ? day.places : day.stops || [];
      stops.forEach((stop, index) => {
        const place = typeof stop === 'string' ? { name: stop.replace(/^\d{1,2}:\d{2}\s*/, '').trim() } : stop;
        if (place.kind === 'stay') lodgings.push(place);
        else visits.push({ ...place, id: place.id || `stop-${day.day}-${index}`, kind: 'course' });
      });
      const savedStays = day.stays || (day.lodging ? [day.lodging] : []);
      lodgings.push(...savedStays.map((item) => typeof item === 'string' ? { name: item } : item));

      const visitDate = day.visitDate ? Date.parse(day.visitDate) : checkIn + (day.day - 1) * 86400000;
      if (stay && visitDate >= checkIn && visitDate < checkOut) {
        const bookedIndex = lodgings.findIndex((item) => (stay.id != null && item.id === stay.id) || item.name === stay.name);
        if (bookedIndex === -1) lodgings.push(stay);
        else lodgings[bookedIndex] = { ...stay, ...lodgings[bookedIndex], imageUrls: stay.imageUrls?.length ? stay.imageUrls : lodgings[bookedIndex].imageUrls };
      }
      return { day: day.day, places: [...visits, ...lodgings.map((item, index) => ({
        ...item, id: `stay-${day.day}-${index}`, kind: 'stay',
      }))] };
    });
  }, [plan, stay, travelDates]);

  return <ItineraryMapPanel
    days={days}
    center={centers[region] || Object.entries(centers).find(([name]) => String(region || '').includes(name))?.[1]}
    searchRegion={region}
    level={5}
    height="320px"
    showIntro={!preview}
    showCategories={false}
    showPlaceList={!preview}
    renderPlaceDetails={preview ? undefined : renderPlacePhoto}
    allDaysLabel={allDaysLabel}
    sourceNote="카카오 지도 · 주소와 장소명은 실제 장소와 다를 수 있습니다. 점선은 방문지 사이를 직선으로 잇습니다."
  />;
}
