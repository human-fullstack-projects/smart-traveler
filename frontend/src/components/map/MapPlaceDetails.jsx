import { useCallback, useEffect, useState } from 'react';
import TourPhoto from '../media/TourPhoto';

function descriptionText(value) {
  return String(value || '').replace(/<br\s*\/?\s*>/gi, '\n').replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;/g, "'").trim();
}

export default function MapPlaceDetails({ place, region, loadDetails, loadPhotos }) {
  const [state, setState] = useState({ status: 'loading' });
  const placeId = state.data?.placeId ?? place.placeId;
  const name = state.data?.name || place.name;
  const address = state.data?.address || place.address;
  const load = useCallback(() => loadPhotos({ placeId, name, address }, region), [loadPhotos, placeId, name, address, region]);
  useEffect(() => {
    let cancelled = false;
    setState({ status: 'loading' });
    loadDetails({ placeId: place.placeId, name: place.name }, region).then(data => {
      if (!cancelled) setState({ status: 'ready', data });
    }).catch(error => {
      if (!cancelled) setState({ status: error.status === 401 ? 'login' : 'missing' });
    });
    return () => { cancelled = true; };
  }, [place.placeId, place.name, region, loadDetails]);
  if (state.status === 'loading') return <p role="status">장소 사진과 설명을 불러오는 중입니다.</p>;
  if (state.status === 'login') return <p>장소 상세 정보는 로그인 후 확인해주세요.</p>;
  const detail = state.data || {};
  const description = descriptionText(detail.description);
  const openingHours = descriptionText(detail.openingHours);
  const hasDetails = Boolean(description || detail.phone || openingHours);
  return <div className={hasDetails ? 'map-place-detail__content' : undefined}>
    <TourPhoto localUrl={detail.imageUrl} alt={`${name} 사진`} load={loadPhotos ? load : undefined}
      missing="등록된 장소 사진이 없습니다." />
    {hasDetails && <div>
      {description && <p className="map-place-detail__description">{description}</p>}
      {detail.phone && <p>연락처: {detail.phone}</p>}
      {openingHours && <p>이용 시간: {openingHours}</p>}
    </div>}
  </div>;
}
