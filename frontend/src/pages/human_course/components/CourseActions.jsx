import { useRef, useState } from 'react';

const reportReasons = ['광고·홍보', '욕설·비방', '부적절한 내용', '기타'];

export default function CourseActions({ course, execute }) {
  const [notice, setNotice] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [liking, setLiking] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [reportError, setReportError] = useState('');
  const reportDialog = useRef(null);

  async function changeLike() {
    if (liking) return;
    setLiking(true);
    setNotice('');
    const result = await execute?.({ type: 'like', id: course.id });
    setNotice(result?.error || (course.likedByMe ? '좋아요를 취소했습니다.' : '이 여행을 좋아요에 추가했습니다.'));
    setLiking(false);
  }

  function openReport() {
    setReportError('');
    reportDialog.current.showModal();
  }

  async function submitReport(event) {
    event.preventDefault();
    if (reporting) return;

    const data = new FormData(event.currentTarget);
    setReporting(true);
    setReportError('');
    try {
      const result = await execute?.({ type: 'report', id: course.id, reason: data.get('reason'), details: data.get('details') });
      if (result?.error) {
        setReportError(result.error);
        return;
      }
      reportDialog.current?.close();
      setNotice('신고를 접수했습니다. 검토 전까지 공개 상태를 유지합니다.');
    } catch {
      setReportError('신고를 접수하지 못했습니다. 다시 시도해 주세요.');
    } finally {
      setReporting(false);
    }
  }

  return <section className="panel"><div className="panel-heading"><h2>게시글 기능</h2></div>
    <div className="actions">
      <button className="primary" type="button" onClick={changeLike} disabled={course.mine || liking} aria-pressed={course.likedByMe} aria-busy={liking}>{liking ? '처리 중…' : course.likedByMe ? '좋아요 취소' : '좋아요 누르기'}</button>
      <span className="muted" aria-live="polite">좋아요 {course.likes}개</span>
      {!course.reportedByMe && <button type="button" aria-haspopup="dialog" onClick={openReport} disabled={course.mine || liking}>게시글 신고</button>}
    </div>
    {course.mine && <p className="right-action"><a className="button btn-full" href={`#/human-courses/${course.id}/edit`}>수정하기</a></p>}
    {course.mine && <div className="delete-course"><button type="button" onClick={() => setDeleting(true)}>내 게시글 삭제</button>{deleting && <div className="note" role="group" aria-label="게시글 삭제 확인"><p>게시글과 댓글을 삭제할까요? 삭제 후에는 되돌릴 수 없습니다.</p><div className="actions"><button type="button" onClick={async () => { const result = await execute?.({ type: 'delete', id: course.id }); if (result?.error) setNotice(result.error); else if (result?.id) window.location.hash = '/human-courses'; }}>삭제 확인</button><button type="button" onClick={() => setDeleting(false)}>취소</button></div></div>}</div>}
    {!course.mine && <dialog ref={reportDialog} className="course-report-dialog" aria-labelledby="course-report-title"
      onCancel={(event) => { if (reporting) event.preventDefault(); }}
      onClose={(event) => event.currentTarget.querySelector('form').reset()}>
      <h2 id="course-report-title">게시글 신고</h2>
      <form onSubmit={submitReport}>
        <fieldset disabled={reporting}>
          <div className="field"><label>신고 사유 (필수)
            <select name="reason" required defaultValue="">
              <option value="">선택</option>
              {reportReasons.map((reason) => <option key={reason}>{reason}</option>)}
            </select>
          </label></div>
          <div className="field"><label>상세 사유 (선택)<textarea name="details" rows={4} maxLength={300} /></label></div>
          <p className="helper">같은 게시글은 한 번만 신고할 수 있습니다.</p>
          {reportError && <p className="form-error" role="alert">{reportError}</p>}
          <div className="actions course-report-actions">
            <button type="button" onClick={() => reportDialog.current.close()}>취소</button>
            <button className="primary" type="submit">{reporting ? '접수 중…' : '신고 접수'}</button>
          </div>
        </fieldset>
      </form>
    </dialog>}
    {course.reportedByMe && <p className="status">이미 신고한 게시글입니다.</p>}
    {notice && <p className="status" role={notice.includes('필요') || notice.includes('못') ? 'alert' : 'status'}>{notice}</p>}
  </section>;
}
