import { useState } from 'react';

export default function CourseActions({ course, execute }) {
  const [notice, setNotice] = useState('');
  const [deleting, setDeleting] = useState(false);
  return <section className="panel"><div className="panel-heading"><h2>게시글 기능</h2></div>
    <div className="actions"><button className="primary" onClick={async () => { const result = await execute?.({ type: 'like', id: course.id }); setNotice(result?.error || '좋아요 상태를 변경했습니다.'); }} aria-pressed={course.likedByMe}>{course.likedByMe ? '좋아요 취소' : '좋아요 누르기'}</button><span className="muted">좋아요 {course.likes}개</span></div>
    {course.mine && <p className="right-action"><a className="button btn-full" href={`#/human-courses/${course.id}/edit`}>내 코스 수정하기 →</a></p>}
    {course.mine && <div className="delete-course"><button type="button" onClick={() => setDeleting(true)}>내 게시글 삭제</button>{deleting && <div className="note" role="group" aria-label="게시글 삭제 확인"><p>게시글과 댓글을 삭제할까요? 삭제 후에는 되돌릴 수 없습니다.</p><div className="actions"><button type="button" onClick={async () => { const result = await execute?.({ type: 'delete', id: course.id }); if (result?.error) setNotice(result.error); else if (result?.id) window.location.hash = '/human-courses'; }}>삭제 확인</button><button type="button" onClick={() => setDeleting(false)}>취소</button></div></div>}</div>}
    <hr className="divider" />
    {!course.mine && !course.reportedByMe && <details><summary>게시글 신고</summary><form onSubmit={async (event) => { event.preventDefault(); const data = new FormData(event.currentTarget); const result = await execute?.({ type: 'report', id: course.id, reason: data.get('reason'), details: data.get('details') }); setNotice(result?.error || '신고를 접수했습니다. 검토 전까지 공개 상태를 유지합니다.'); }}>
      <div className="field"><label>신고 사유 *<select name="reason" required defaultValue=""><option value="">선택</option>{['광고·홍보', '욕설·비방', '부적절한 내용', '기타'].map((value) => <option key={value}>{value}</option>)}</select></label></div>
      <div className="field"><label>상세 사유 (선택)<textarea name="details" maxLength={300} /></label></div><button type="submit">신고 접수</button>
    </form></details>}
    <p className="helper">본인 글은 신고할 수 없으며, 같은 글은 한 번만 신고할 수 있습니다.</p>
    {course.reportedByMe && <p className="status">이미 신고한 게시글입니다.</p>}
    {notice && <p className="status" role="status">{notice}</p>}
  </section>;
}
