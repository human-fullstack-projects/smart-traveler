const centers = {
  부산: [35.1796, 129.0756], 강릉: [37.7519, 128.8761], 서울: [37.5665, 126.9780],
  제주: [33.4996, 126.5312], 경주: [35.8562, 129.2247],
};

export default function CourseMap({ region, plan = [] }) {
  const center = centers[region];
  if (!center) return <p className="note">이 지역의 지도 미리보기가 아직 준비되지 않았습니다.</p>;
  const [latitude, longitude] = center;
  const bbox = [longitude - 0.08, latitude - 0.05, longitude + 0.08, latitude + 0.05].join(',');
  const mapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${encodeURIComponent(bbox)}&layer=mapnik&marker=${encodeURIComponent(`${latitude},${longitude}`)}`;
  return <div className="course-map">
    <p className="form-help">{region} 지역 중심 지도입니다. 아래 방문지를 누르면 지도에서 위치를 검색할 수 있습니다.</p>
    <iframe title={`${region} 지역 지도`} src={mapUrl} loading="lazy" referrerPolicy="no-referrer" />
    <ul className="map-stops">{plan.flatMap((day) => day.stops.map((stop, index) => ({ day: day.day, stop, index }))).map(({ day, stop, index }) => {
      const place = stop.replace(/^\d{1,2}:\d{2}\s*/, '').trim();
      return <li key={`${day}-${index}`}><a href={`https://www.openstreetmap.org/search?query=${encodeURIComponent(`${region} ${place}`)}`} target="_blank" rel="noopener noreferrer">{day}일차 · {place} ↗</a></li>;
    })}</ul>
  </div>;
}
