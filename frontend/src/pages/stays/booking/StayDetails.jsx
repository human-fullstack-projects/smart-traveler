import { useEffect, useMemo, useState } from 'react';
import KakaoMap from '../../../components/map/KakaoMap.jsx';
import { loadKakaoMaps } from '../../../components/map/loadKakaoMaps.js';
import { addressSearch, hasValidCoordinates } from '../../../components/map/resolveKakaoPlace.js';
import StayPhotoGallery from './StayPhotoGallery.jsx';

function readableText(value) {
  return String(value || '').replace(/<\/?(?:br|p)\s*\/?>/gi, '\n').replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#39;/g, "'").trim()
    .replace(/^(?:\[\s*예시(?:\s+[^\]\n]*)?\s*\]\s*)+/, '');
}

function websiteUrl(value) {
  try {
    const url = new URL(value);
    return ['https:', 'http:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

export function StayCover({ stay }) {
  const [cover, ...extraStayPhotos] = stay.photos ?? (stay.photoUrl || stay.thumbnailUrl
    ? [{ imageUrl: stay.photoUrl, thumbnailUrl: stay.thumbnailUrl }] : []);
  const seenUrls = new Set(cover ? [cover.imageUrl || cover.thumbnailUrl] : []);
  const photos = [...extraStayPhotos, ...(stay.roomPhotos ?? [])].filter((photo) => {
    const url = photo.imageUrl || photo.thumbnailUrl;
    if (!url || seenUrls.has(url)) return false;
    seenUrls.add(url);
    return true;
  });
  return <StayPhotoGallery title={stay.name} cover={cover} photos={photos} />;
}

export function StayOverview({ stay }) {
  const description = readableText(stay.description);
  const parking = readableText(stay.parkingInfo);
  const refund = readableText(stay.refundPolicy);
  const amenities = [...new Set(readableText(stay.amenities).split(/[,\n]/).map(item => item.trim()).filter(Boolean))];
  const website = websiteUrl(stay.homepageUrl);

  return <section className="booking-section booking-stay-overview" aria-labelledby="booking-overview-title">
    <h2 id="booking-overview-title">숙소 소개</h2>
    <p className="booking-stay-copy">{description || '등록된 숙소 소개가 없습니다.'}</p>
    <h3>이용 안내</h3>
    <dl className="booking-stay-facts">
      <div><dt>체크인</dt><dd>{stay.checkinTime?.slice(0, 5) || '숙소 문의'}</dd></div>
      <div><dt>체크아웃</dt><dd>{stay.checkoutTime?.slice(0, 5) || '숙소 문의'}</dd></div>
      {parking && <div><dt>주차</dt><dd>{parking}</dd></div>}
      {stay.phone?.trim() && <div><dt>연락처</dt><dd>{stay.phone}</dd></div>}
      {website && <div><dt>홈페이지</dt><dd><a href={website} target="_blank" rel="noopener noreferrer">숙소 홈페이지</a></dd></div>}
    </dl>
    {amenities.length > 0 && <div className="booking-stay-detail">
      <h3>편의시설</h3>
      <ul className="booking-stay-amenities">{amenities.map(item => <li key={item}>{item}</li>)}</ul>
    </div>}
    {refund && <div className="booking-stay-detail">
      <h3>취소 및 환불</h3>
      <p className="booking-stay-copy">{refund}</p>
    </div>}
  </section>;
}

export function StayLocation({ stay, courseRoute }) {
  const [addressLocation, setAddressLocation] = useState(null);
  const hasCoordinates = hasValidCoordinates(stay);
  const address = stay.address?.trim();

  useEffect(() => {
    if (hasCoordinates || !address) return;
    let active = true;
    loadKakaoMaps().then(maps => addressSearch(maps, address)).then(result => {
      if (!active) return;
      const position = { latitude: result?.y, longitude: result?.x };
      setAddressLocation({ address, position: hasValidCoordinates(position) ? position : null });
    }).catch(() => {
      if (active) setAddressLocation({ address, position: null });
    });
    return () => { active = false; };
  }, [address, hasCoordinates]);

  const places = useMemo(() => {
    const position = hasCoordinates ? stay : addressLocation?.address === address && addressLocation?.position;
    if (!position) return [];
    return [{ id: stay.lodgingId, name: stay.name, kind: 'stay', markerLabel: '숙소',
      latitude: position.latitude, longitude: position.longitude }];
  }, [stay, address, addressLocation, hasCoordinates]);
  const locating = !hasCoordinates && address && addressLocation?.address !== address;
  const mapPlaces = useMemo(() => courseRoute ? [...places, ...courseRoute.places] : places, [places, courseRoute]);

  return <section className="booking-section booking-stay-location" aria-labelledby="booking-location-title">
    <h2 id="booking-location-title">{courseRoute ? '여행 일정과 숙소 위치' : '숙소 위치'}</h2>
    {address && <p className="booking-stay-address">{[address, stay.addressDetail, stay.zipcode && `(${stay.zipcode})`].filter(Boolean).join(' ')}</p>}
    <div className="booking-stay-map">
      {mapPlaces.length > 0
        ? <KakaoMap places={mapPlaces} paths={courseRoute?.paths} fitPoints={courseRoute ? mapPlaces : undefined} level={4} height="100%" />
        : <p role={locating ? 'status' : undefined}>{locating ? '숙소 위치를 확인하고 있습니다.' : '지도에 표시할 숙소 위치를 확인하지 못했습니다.'}</p>}
    </div>
  </section>;
}
