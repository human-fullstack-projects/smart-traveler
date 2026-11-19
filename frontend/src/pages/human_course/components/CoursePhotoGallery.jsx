import { useId, useRef, useState } from 'react';

const previewLimit = 4;

function GalleryPhoto({ photo, alt, lazy = false }) {
  const [failedUrl, setFailedUrl] = useState(null);
  return photo.dataUrl && photo.dataUrl !== failedUrl
    ? <img src={photo.dataUrl} alt={alt} loading={lazy ? 'lazy' : undefined} onError={() => setFailedUrl(photo.dataUrl)} />
    : <span className="course-photo-placeholder">{photo.name || '사진'}<small>이미지를 표시할 수 없습니다.</small></span>;
}

export default function CoursePhotoGallery({ title, photos }) {
  const dialog = useRef(null);
  const headingId = useId();
  const [activeIndex, setActiveIndex] = useState(0);
  const extraCount = photos.length - previewLimit;
  const photo = photos[activeIndex];

  function openGallery(index) {
    setActiveIndex(index);
    dialog.current.showModal();
  }

  function step(direction) {
    setActiveIndex((index) => (index + direction + photos.length) % photos.length);
  }

  function handleGalleryKey(event) {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    step(event.key === 'ArrowLeft' ? -1 : 1);
  }

  return <section className="course-gallery" aria-labelledby={headingId}>
    <h3 id={headingId}>등록 사진</h3>
    {photos.length ? <div className="course-gallery-grid">
      {photos.slice(0, previewLimit).map((item, index) => {
        const showMore = index === previewLimit - 1 && extraCount > 0;
        return <button className="course-gallery-tile" key={index} type="button"
          aria-label={showMore ? `사진 ${extraCount}장 더 보기` : `사진 ${index + 1} 크게 보기`}
          onClick={() => openGallery(showMore ? previewLimit : index)}>
          <GalleryPhoto photo={item} alt="" lazy />
          {showMore && <span className="course-gallery-more">+{extraCount} 더 보기</span>}
        </button>;
      })}
    </div> : <p className="muted">첨부된 사진이 없습니다.</p>}
    {photos.length > 0 && <dialog className="course-gallery-dialog" ref={dialog} aria-label={`${title} 등록 사진`} onKeyDown={handleGalleryKey}>
      <div className="course-gallery-header">
        <h3>{title}</h3>
        <button type="button" aria-label="사진 닫기" onClick={() => dialog.current.close()}>×</button>
      </div>
      <div className="course-gallery-stage">
        {photo && <GalleryPhoto key={activeIndex} photo={photo} alt={`${title} 사진 ${activeIndex + 1}`} />}
        {photos.length > 1 && <>
          <button type="button" className="course-gallery-prev" aria-label="이전 사진" onClick={() => step(-1)}>‹</button>
          <button type="button" className="course-gallery-next" aria-label="다음 사진" onClick={() => step(1)}>›</button>
        </>}
        <span className="course-gallery-count" role="status">{activeIndex + 1} / {photos.length}</span>
      </div>
      {photos.length > 1 && <div className="course-gallery-thumbnails" role="group" aria-label="사진 선택">
        {photos.map((item, index) => <button key={index} type="button" aria-label={`사진 ${index + 1} 보기`}
          aria-current={index === activeIndex ? 'true' : undefined} onClick={() => setActiveIndex(index)}>
          <GalleryPhoto photo={item} alt="" lazy />
        </button>)}
      </div>}
    </dialog>}
  </section>;
}
