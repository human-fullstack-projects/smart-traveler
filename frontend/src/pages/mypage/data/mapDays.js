import { hasValidCoordinates } from '../../../components/map/resolveKakaoPlace.js';

const cleanName = value => String(value || '').replace(/^\d{1,2}:\d{2}\s*/, '').trim();
const nameKey = value => cleanName(value).replace(/\s+/g, '').toLowerCase();

export function mapDays(days = [], coordinateDays = []) {
  return days.map((day, dayIndex) => {
    const coordinates = coordinateDays[dayIndex]?.mapPlaces || [];
    const names = day.places?.length ? day.places : day.stops || [];
    const places = names.length ? names : day.mapPlaces || [];
    return {
      day: day.day || dayIndex + 1,
      places: places.map((place, index) => {
        const source = typeof place === 'string' ? { name: cleanName(place) } : place;
        const matches = [...(day.mapPlaces || []), ...coordinates]
          .filter(point => nameKey(point.name) === nameKey(source.name));
        // 후기의 옛 일정은 유지하고 이름이 일치하는 장소의 위치 정보만 보완한다.
        const point = matches.find(hasValidCoordinates) || matches[0];
        return { ...point, ...source,
          address: source.address || point?.address,
          latitude: source.latitude ?? point?.latitude,
          longitude: source.longitude ?? point?.longitude,
          id: `mypage-${dayIndex}-${index}`, kind: 'course' };
      }),
    };
  });
}
