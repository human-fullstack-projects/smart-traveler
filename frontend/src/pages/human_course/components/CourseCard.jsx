import { useState } from 'react';

export default function CourseCard({ course, detailHref }) {
  const titleId = `human-course-title-${course.id}`;
  const CardBody = detailHref ? 'a' : 'div';
  const [failedUrls, setFailedUrls] = useState([]);
  const routePhotos = (course.plan || []).flatMap(day => (day.places || []).flatMap(place =>
    place?.kind === 'stay' ? [] : place?.imageUrls || []));
  const cover = [course.coverPhotoUrl, ...(course.photoAssets || []).map(photo => photo.dataUrl),
    ...routePhotos, ...(course.stay?.imageUrls || [])].find(url => url?.trim() && !failedUrls.includes(url));
  return <article className="course-card" aria-labelledby={titleId}>
    <CardBody className="course-card-body" href={detailHref} aria-labelledby={detailHref ? titleId : undefined}>
      <div className="course-thumb">{cover ? <img key={cover} src={cover} alt={`${course.title} 대표 사진`} loading="lazy"
        onError={() => setFailedUrls(urls => [...urls, cover])} /> : <span>등록 사진 {course.photos?.length || 0}장</span>}</div>
      <div className="course-content">
        <div className="course-tags"><span className="badge">{course.region}</span><span className="badge badge-neutral">{course.category}</span><span className="badge badge-neutral">{course.companion}</span></div>
        <h3 id={titleId}>{course.title}</h3>
        <dl className="course-meta-list"><div><dt>작성자</dt><dd>{course.author}</dd></div><div><dt>여행 기간</dt><dd>{course.travelDates || '미등록'}</dd></div><div><dt>작성일</dt><dd><time dateTime={course.createdAt}>{course.createdAt}</time></dd></div></dl>
        <div className="course-foot"><span className="course-like-count" aria-label={`좋아요 ${course.likes}개`}>♥ {course.likes}</span>{detailHref && <span className="course-detail-link">여행 상세 보기 →</span>}</div>
      </div>
    </CardBody>
  </article>;
}
