import { useRef, useState } from 'react';
import { TourPhotoCredit, TourPhotoStatus } from '../../../components/media/TourPhoto';
import { getStaySamplePhotos } from './staySamplePhotos.js';

const previewLimit = 4;
const photoUrl = photo => photo?.imageUrl || photo?.thumbnailUrl;

function Photo({ photo, alt, thumbnail = false, onUnavailable }) {
  const [failedUrls, setFailedUrls] = useState([]);
  const urls = thumbnail ? [photo?.thumbnailUrl, photo?.imageUrl] : [photo?.imageUrl, photo?.thumbnailUrl];
  const url = urls.find(candidate => candidate && !failedUrls.includes(candidate));
  return url
    ? <img src={url} alt={alt} loading={thumbnail ? 'lazy' : undefined} referrerPolicy="no-referrer"
        onError={() => {
          setFailedUrls(previous => [...previous, url]);
          if (!urls.some(candidate => candidate && candidate !== url && !failedUrls.includes(candidate))) onUnavailable?.();
        }} />
    : <span>사진을 불러오지 못했습니다.</span>;
}

export default function StayPhotoGallery({ title, stayId, cover, photos = [], room = false, photoStatus }) {
  const dialog = useRef(null);
  const [activeUrl, setActiveUrl] = useState(null);
  const [failedPhotos, setFailedPhotos] = useState([]);
  const actualPhotos = (room ? photos : [cover, ...photos]).filter(photo => photo && !failedPhotos.includes(photoUrl(photo)));
  const [currentCover, ...extraPhotos] = actualPhotos;
  if (!room && photoStatus !== 'loading' && extraPhotos.length < previewLimit) {
    const samples = getStaySamplePhotos(stayId);
    const available = samples.filter(photo => !failedPhotos.includes(photoUrl(photo)));
    extraPhotos.push(...available.slice(0, samples.length - extraPhotos.length));
  }
  const gallery = currentCover ? [currentCover, ...extraPhotos] : extraPhotos;
  const coverOffset = currentCover ? 1 : 0;
  const preview = extraPhotos.slice(0, previewLimit);
  const hasMultiple = gallery.length > 1;
  const extraCount = extraPhotos.length - preview.length;
  const activeIndex = activeUrl == null ? -1 : Math.max(0, gallery.findIndex(photo => photoUrl(photo) === activeUrl));
  const activePhoto = gallery[activeIndex];

  function markUnavailable(photo) {
    setFailedPhotos(previous => [...previous, photoUrl(photo)]);
  }

  function openPhotos(index) {
    setActiveUrl(photoUrl(gallery[index]));
    dialog.current.showModal();
  }

  function step(direction) {
    setActiveUrl(photoUrl(gallery[(activeIndex + direction + gallery.length) % gallery.length]));
  }

  function handleKeyDown(event) {
    if (!hasMultiple || (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight')) return;
    event.preventDefault();
    step(event.key === 'ArrowLeft' ? -1 : 1);
  }

  return <>
    {room ? (currentCover ? <button className="booking-room-photo" type="button" aria-label={`${title} 사진 보기`}
      aria-haspopup="dialog" onClick={() => openPhotos(0)}>
      <Photo key={photoUrl(currentCover)} photo={currentCover} alt="" thumbnail onUnavailable={() => markUnavailable(currentCover)} />
    </button> : <div className="booking-room-photo">등록된 객실 사진이 없습니다.</div>) : <>
    <div className="booking-stay-photo-grid" role="group" aria-label="숙소 사진">
      <div className="booking-stay-photo-tile booking-stay-photo-cover">
        {currentCover ? <button className="booking-stay-photo" type="button" aria-haspopup="dialog"
            aria-label="숙소 대표 사진 크게 보기" onClick={() => openPhotos(0)}>
            <Photo key={photoUrl(currentCover)} photo={currentCover} alt=""
              onUnavailable={() => markUnavailable(currentCover)} />
        </button> : <div className="booking-stay-photo-status"><TourPhotoStatus status={photoStatus}
          missing={failedPhotos.length ? '사진을 표시할 수 없습니다.' : '등록된 숙소 대표 사진이 없습니다.'} /></div>}
      </div>
      <div className="booking-stay-photo-extras" role="group" aria-label="추가 숙소 및 객실 사진"
        aria-busy={photoStatus === 'loading'}>
        {Array.from({ length: previewLimit }, (_, index) => {
          const photo = preview[index];
          return <div key={index} className="booking-stay-photo-tile">
            {photo ? <button className="booking-stay-photo" type="button" aria-haspopup="dialog"
              aria-label={`추가 사진 ${index + 1} 크게 보기`}
              onClick={() => openPhotos(index + coverOffset)}>
              <Photo key={photoUrl(photo)} photo={photo} alt="" thumbnail onUnavailable={() => markUnavailable(photo)} />
            </button> : photoStatus !== 'loading' && <span className="booking-stay-photo-empty">사진 없음</span>}
            {index === previewLimit - 1 && extraCount > 0 && <button className="booking-stay-photo-more" type="button"
              aria-haspopup="dialog" aria-label={`추가 사진 ${extraCount}장 더 보기`}
              onClick={() => openPhotos(previewLimit + coverOffset)}>+{extraCount}</button>}
          </div>;
        })}
      </div>
    </div>
    {gallery.some(photo => photo.tour) && <div className="booking-stay-photo-credit"><TourPhotoCredit /></div>}
    </>}
    {gallery.length > 0 && <dialog ref={dialog} className="booking-room-photo-dialog"
      aria-label={`${title} ${room ? '사진' : '숙소 사진'}`} onKeyDown={handleKeyDown} onClose={() => setActiveUrl(null)}>
      <div className="booking-room-photo-header">
        <h3>{title}</h3>
        <button type="button" aria-label="사진 닫기" onClick={() => dialog.current.close()}>×</button>
      </div>
      {activePhoto && <><div className="booking-room-photo-stage">
        <Photo key={photoUrl(activePhoto)} photo={activePhoto}
          alt={`${title} 사진 ${activeIndex + 1}`} />
        {hasMultiple && <>
          <button className="booking-room-photo-prev" type="button" aria-label="이전 사진" onClick={() => step(-1)}>‹</button>
          <button className="booking-room-photo-next" type="button" aria-label="다음 사진" onClick={() => step(1)}>›</button>
          <span className="booking-room-photo-count" role="status">{activeIndex + 1} / {gallery.length}</span>
        </>}
      </div>{activePhoto.tour && <div className="booking-room-photo-credit"><TourPhotoCredit /></div>}</>}
    </dialog>}
  </>;
}
