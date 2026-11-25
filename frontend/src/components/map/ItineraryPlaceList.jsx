import MapPlaceCards from './MapPlaceCards';
import './itinerary-map-panel.css';

export default function ItineraryPlaceList({ days, ...cardProps }) {
  return <div className="itinerary-map__day-list">{days.map(day => <section className="itinerary-map__day" key={day.day}>
    <h3>{day.day}일차</h3>
    {day.places.length ? <MapPlaceCards places={day.places} {...cardProps} /> : <p>표시할 장소가 없습니다.</p>}
  </section>)}</div>;
}
