export default function PageHeading({ title, description, breadcrumb, children }) {
  return <>
    {breadcrumb && <p className="breadcrumb"><a href="#/human-courses">코스 목록</a> / {breadcrumb}</p>}
    <div className="page-heading"><div><p className="small-kicker">HUMAN COURSES</p><h1>{title}</h1><p className="subheading">{description}</p></div>{children}</div>
    <p className="mock-notice">화면 확인용 샘플 · 나(샘플 사용자) · 기준 시각 2026-10-26 12:00 (한국) · 서버·실제 이미지 업로드 없음</p>
    <p className="helper">변경 내용은 이 브라우저에 저장됩니다. 사진은 파일명만 보관하며 실제 이미지는 업로드하지 않습니다.</p>
  </>;
}
