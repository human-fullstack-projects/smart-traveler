import { Fragment } from 'react';

export default function TripTimeline({ course, nights = 1 }) {
  const places = course?.places || [];
  const days = Number(nights) + 1;
  return Array.from({ length: days }, (_, index) => {
    const day = index + 1;
    const entries = day === 1
      ? [['10:30', places[0]], ['14:30', places[1]], ['15:00', '숙소 체크인'], ['17:00', places[2]]]
      : day === days
        ? [['09:00', '숙소 주변 아침 식사'], ['11:00', '체크아웃 · 귀가']]
        : [['10:00', places[1]], ['12:30', '지역 식당'], ['15:00', places[2]]];
    return <Fragment key={day}><h3>{day}일차</h3><ul className="timeline">{entries.map(([time, place]) => <li key={time}><strong>{time}</strong><p>{place}</p></li>)}</ul></Fragment>;
  });
}
