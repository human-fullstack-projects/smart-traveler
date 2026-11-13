import { useId } from 'react';
import { RATING_LABELS } from '../data/preferenceRating';

export default function PreferenceGauge({ name, rating, onChange }) {
  const id = useId();
  return <div className={`mp-rating-row${rating === null ? ' is-unset' : ''}`}>
    <div className="mp-rating-name"><label htmlFor={id}>{name === '가족(아이동반)' ? <>가족<span className="mp-rating-name-detail">(아이동반)</span></> : name}</label><span>{rating === null ? '미선택' : `${rating}단계 · ${RATING_LABELS[rating - 1]}`}</span></div>
    <div className="mp-rating-gauge">
      <input id={id} className="mp-rating-slider" style={{ '--rating-fill': `${rating === null ? 0 : (rating - 1) * 25}%` }} type="range" min="1" max="5" step="1" value={rating ?? 1} aria-valuetext={rating === null ? '미선택' : `${rating}단계 ${RATING_LABELS[rating - 1]}`} onChange={event => onChange(Number(event.target.value))} onPointerDown={() => { if (rating === null) onChange(1); }} />
      <div className="mp-rating-steps" role="group" aria-label={`${name} 선호 단계`}>
        {RATING_LABELS.map((label, index) => <button type="button" key={label} aria-label={`${name} ${index + 1}단계 ${label}`} aria-pressed={rating === index + 1} onClick={() => onChange(index + 1)}><strong>{index + 1}</strong><span>{label}</span></button>)}
      </div>
    </div>
    <button type="button" className="mp-rating-clear" aria-label={`${name} 미선택으로 변경`} aria-pressed={rating === null} onClick={() => onChange(null)}>미선택</button>
  </div>;
}
