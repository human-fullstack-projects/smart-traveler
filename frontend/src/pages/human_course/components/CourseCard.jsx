export default function CourseCard({ course, detailHref }) {
  const titleId = `human-course-title-${course.id}`;
  return <article className="course-card" aria-labelledby={titleId}>
    <div className="course-thumb">{course.photoAssets?.[0]?.dataUrl ? <img src={course.photoAssets[0].dataUrl} alt={`${course.title} 대표 사진`} loading="lazy" /> : <span>등록 사진 {course.photos?.length || 0}장</span>}</div>
    <div className="course-content">
      <div className="course-tags"><span className="badge">{course.region}</span><span className="badge badge-neutral">{course.category}</span><span className="badge badge-neutral">{course.companion}</span></div>
      <h3 id={titleId}>{detailHref ? <a href={detailHref}>{course.title}</a> : course.title}</h3><p className="course-summary">{course.summary}</p>
      <dl className="course-meta-list"><div><dt>작성자</dt><dd>{course.author}</dd></div><div><dt>여행 기간</dt><dd>{course.travelDates || '미등록'}</dd></div><div><dt>작성일</dt><dd><time dateTime={course.createdAt}>{course.createdAt}</time></dd></div></dl>
      <div className="course-foot"><span className="course-like-count" aria-label={`좋아요 ${course.likes}개`}>♥ {course.likes}</span>{detailHref && <a className="course-detail-link" href={detailHref} aria-label={`${course.title} 상세 보기`}>코스 상세 보기 →</a>}</div>
    </div>
  </article>;
}
