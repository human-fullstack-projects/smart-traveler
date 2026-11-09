export default function CourseCard({ course, detailHref }) {
  return <article className="course-card">
    <div className="course-thumb">{course.photoAssets?.[0]?.dataUrl ? <img src={course.photoAssets[0].dataUrl} alt={`${course.title} 대표 사진`} /> : <span>등록 사진 {course.photos?.length || 0}장</span>}</div>
    <div className="course-content">
      <div className="course-tags"><span className="badge">{course.region}</span><span className="badge badge-neutral">{course.category}</span><span className="badge badge-neutral">{course.companion}</span></div>
      <h3>{course.title}</h3><p className="course-summary">{course.summary}</p>
      <p className="course-meta">작성자 {course.author} · 여행 기간 {course.travelDates || '미등록'} · 작성일 {course.createdAt}</p>
      <div className="course-foot"><span className="course-meta">♡ {course.likes}</span>{detailHref && <a href={detailHref}>코스 상세 보기 →</a>}</div>
    </div>
  </article>;
}
