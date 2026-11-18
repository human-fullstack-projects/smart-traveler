import { useEffect, useState } from 'react';

function descriptionText(value) {
  return String(value || '').replace(/<br\s*\/?\s*>/gi, '\n').replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;/g, "'").trim();
}

export default function MapPlaceDetails({ place, region, loadDetails }) {
  const [state, setState] = useState({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);
  const [imageFailed, setImageFailed] = useState(false);
  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' }); setImageFailed(false);
    loadDetails({ placeId: place.placeId, name: place.name }, region).then(data => {
      if (!cancelled) setState({ status: 'ready', data });
    }).catch(error => {
      if (!cancelled) setState({ status: error.status === 404 ? 'missing' : 'error', error });
    });
    return () => { cancelled = true; };
  }, [place.placeId, place.name, region, loadDetails, attempt]);
  if (state.status === 'loading') return <p role="status">장소 사진과 설명을 불러오는 중입니다.</p>;
  if (state.status === 'missing') return <p>이 장소와 일치하는 상세 정보가 DB에 없습니다.</p>;
  if (state.status === 'error') return <p role="alert">{state.error.status === 401 ? '장소 상세 정보는 로그인 후 확인해주세요.' : '장소 정보를 불러오지 못했습니다.'} <button type="button" className="text-button" onClick={() => setAttempt(value => value + 1)}>다시 시도</button></p>;
  const detail = state.data;
  return <div className="map-place-detail__content">
    {detail.imageUrl && !imageFailed ? <img src={detail.imageUrl} alt={`${detail.name} 사진`} onError={() => setImageFailed(true)} /> : <div className="map-place-detail__no-photo">등록된 장소 사진이 없습니다.</div>}
    <div><h4>{detail.name}</h4>{detail.address && <p>{detail.address}</p>}
      <p className="map-place-detail__description">{descriptionText(detail.description) || '등록된 장소 설명이 없습니다.'}</p>
      {detail.phone && <p>연락처: {detail.phone}</p>}
      {detail.openingHours && <p>이용 시간: {descriptionText(detail.openingHours)}</p>}
    </div>
  </div>;
}
