export default function CourseCard({ course, detailHref }) {
  return <article className="course-card">
    <div className="course-thumb">사진 영역 (샘플)</div>
    <div className="course-content">
      <div className="course-tags"><span className="badge">{course.region}</span><span className="badge badge-neutral">{course.category}</span><span className="badge badge-neutral">{course.companion}</span></div>
      <h3>{course.title}</h3><p className="course-summary">{course.summary}</p>
      <p className="course-meta">{course.author} · {course.createdAt}</p>
      <div className="course-foot"><span className="course-meta">♡ {course.likes}</span>{detailHref && <a href={detailHref}>코스 상세 보기 →</a>}</div>
    </div>
  </article>;
}
