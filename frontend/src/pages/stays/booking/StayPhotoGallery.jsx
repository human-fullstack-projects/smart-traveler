import { useRef, useState } from 'react';

const previewLimit = 4;

function Photo({ photo, alt, thumbnail = false }) {
  const [failedUrls, setFailedUrls] = useState([]);
  const urls = thumbnail ? [photo?.thumbnailUrl, photo?.imageUrl] : [photo?.imageUrl, photo?.thumbnailUrl];
  const url = urls.find(candidate => candidate && !failedUrls.includes(candidate));
  return url
    ? <img src={url} alt={alt} loading={thumbnail ? 'lazy' : undefined} referrerPolicy="no-referrer"
        onError={() => setFailedUrls(previous => [...previous, url])} />
    : <span>{photo ? '사진을 불러오지 못했습니다.' : thumbnail ? '객실 사진 준비 중' : '등록된 객실 사진이 없습니다.'}</span>;
}

export default function StayPhotoGallery({ title, cover, photos = [], room = false }) {
  const dialog = useRef(null);
  const [activeIndex, setActiveIndex] = useState(null);
  const gallery = cover ? [cover, ...photos] : photos;
  const hasMultiple = gallery.length > 1;
  const offset = cover ? 1 : 0;
  const extraCount = photos.length - previewLimit;

  function openPhotos(index) {
    setActiveIndex(index);
    dialog.current.showModal();
  }

  function step(direction) {
    setActiveIndex(index => (index + direction + gallery.length) % gallery.length);
  }

  function handleKeyDown(event) {
    if (!hasMultiple || (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight')) return;
    event.preventDefault();
    step(event.key === 'ArrowLeft' ? -1 : 1);
  }

  return <>
    {room ? <button className="booking-room-photo" type="button" aria-label={`${title} 사진 보기`}
      aria-haspopup="dialog" onClick={() => openPhotos(0)}>
      <Photo photo={photos[0]} alt="" thumbnail />
    </button> : <div className="booking-stay-photo-grid" role="group" aria-label="숙소 사진">
      <div className="booking-stay-photo-tile booking-stay-photo-cover">
        {cover ? <button className="booking-stay-photo" type="button" aria-haspopup="dialog"
          aria-label="숙소 대표 사진 크게 보기" onClick={() => openPhotos(0)}>
          <Photo photo={cover} alt="" />
        </button> : <p>등록된 숙소 대표 사진이 없습니다.</p>}
      </div>
      <div className="booking-stay-photo-extras" role="group" aria-label="추가 숙소 및 객실 사진">
        {Array.from({ length: previewLimit }, (_, index) => <div key={index} className="booking-stay-photo-tile"
          aria-hidden={!photos[index] || undefined}>
          {photos[index] && <button className="booking-stay-photo" type="button" aria-haspopup="dialog"
            aria-label={`추가 사진 ${index + 1} 크게 보기`} onClick={() => openPhotos(index + offset)}>
            <Photo photo={photos[index]} alt="" thumbnail />
          </button>}
          {index === previewLimit - 1 && extraCount >= 0 && <button className="booking-stay-photo-more" type="button"
            aria-haspopup="dialog" aria-label={extraCount ? `추가 사진 ${extraCount}장 더 보기` : '추가 사진 전체 보기'}
            onClick={() => openPhotos(offset + (extraCount ? previewLimit : 0))}>{extraCount ? `+${extraCount}` : '+'}</button>}
        </div>)}
      </div>
    </div>}
    {(room || gallery.length > 0) && <dialog ref={dialog} className={`booking-room-photo-dialog${gallery.length ? '' : ' is-empty'}`}
      aria-label={`${title} ${room ? '객실' : '숙소'} 사진`} onKeyDown={handleKeyDown} onClose={() => setActiveIndex(null)}>
      <div className="booking-room-photo-header">
        <h3>{title}</h3>
        <button type="button" aria-label="사진 닫기" onClick={() => dialog.current.close()}>×</button>
      </div>
      {activeIndex !== null && <div className="booking-room-photo-stage">
        <Photo key={activeIndex} photo={gallery[activeIndex]} alt={`${title} 사진 ${activeIndex + 1}`} />
        {hasMultiple && <>
          <button className="booking-room-photo-prev" type="button" aria-label="이전 사진" onClick={() => step(-1)}>‹</button>
          <button className="booking-room-photo-next" type="button" aria-label="다음 사진" onClick={() => step(1)}>›</button>
          <span className="booking-room-photo-count" role="status">{activeIndex + 1} / {gallery.length}</span>
        </>}
      </div>}
    </dialog>}
  </>;
}
