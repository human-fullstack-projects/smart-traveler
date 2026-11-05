import { useState } from 'react';

export default function CourseActions({ course, execute }) {
  const [notice, setNotice] = useState('');
  return <section className="panel"><div className="panel-heading"><h2>게시글 기능</h2></div>
    <div className="actions"><button className="primary" onClick={() => { const result = execute?.({ type: 'like', id: course.id }); setNotice(result?.error || '좋아요 상태를 변경했습니다.'); }} aria-pressed={course.likedByMe}>{course.likedByMe ? '좋아요 취소' : '좋아요 누르기'}</button><span className="muted">좋아요 {course.likes}개</span></div>
    {course.mine && <p className="right-action"><a className="button btn-full" href={`#/human-courses/${course.id}/edit`}>내 코스 수정하기 →</a></p>}
    <hr className="divider" />
    {!course.mine && !course.reportedByMe && <details><summary>게시글 신고</summary><form onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); const result = execute?.({ type: 'report', id: course.id, reason: data.get('reason'), details: data.get('details') }); setNotice(result?.error || '샘플 신고를 접수했습니다. 검토 전까지 공개 상태를 유지합니다.'); }}>
      <div className="field"><label>신고 사유 *<select name="reason" required defaultValue=""><option value="">선택</option>{['광고·홍보', '욕설·비방', '부적절한 내용', '기타'].map((value) => <option key={value}>{value}</option>)}</select></label></div>
      <div className="field"><label>상세 사유 (선택)<textarea name="details" maxLength={300} /></label></div><button type="submit">신고 접수</button>
    </form></details>}
    <p className="helper">본인 글은 신고할 수 없으며, 같은 글은 한 번만 신고할 수 있습니다.</p>
    {course.reportedByMe && <p className="status">이미 신고한 게시글입니다.</p>}
    {notice && <p className="status" role="status">{notice}</p>}
  </section>;
}
