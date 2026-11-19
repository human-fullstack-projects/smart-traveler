import { useState } from 'react';

export function PlacePhoto({ name, imageUrls = [], emptyMessage }) {
  const [failedUrls, setFailedUrls] = useState([]);
  const photo = imageUrls.find(url => !failedUrls.includes(url));
  if (!photo) return emptyMessage ? <p>{emptyMessage}</p> : null;

  return <img className="plan-place-photo" src={photo} alt={`${name} 사진`} loading="lazy"
    onError={() => setFailedUrls(urls => [...urls, photo])} />;
}

export default function CoursePlan({ plan = [], version, showPhotos = false }) {
  const stopName = (stop) => (typeof stop === 'string' ? stop : stop?.name || '')
    .replace(/^\d{1,2}:\d{2}\s*/, '').trim();
  return <div className="course-plan">
    {version != null && <p className="helper">공유 시 첨부한 최종 코스 v{version} · 읽기 전용</p>}
    {plan.length ? plan.map(day => {
      const stops = showPhotos && day.places?.length ? day.places : day.stops;
      return <section className="plan-day" key={day.day}>
        <h4>{day.day}일차 일정</h4>
        <ol className="plan-stops">{stops.map((stop, index) => {
          const name = stopName(stop);
          return <li key={stop?.id ?? index}>
            <div className="plan-stop-content">
              <span>{name}</span>
              {showPhotos && stop?.imageUrls?.length > 0 && <PlacePhoto name={name} imageUrls={stop.imageUrls} />}
            </div>
          </li>;
        })}</ol>
      </section>;
    }) : <p>불러올 최종 일정이 없습니다.</p>}
  </div>;
}
