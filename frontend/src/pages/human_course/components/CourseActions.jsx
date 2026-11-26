import { useRef, useState } from 'react';

const reportReasons = ['광고·홍보', '욕설·비방', '부적절한 내용', '기타'];

export default function CourseActions({ course, execute }) {
  const [notice, setNotice] = useState('');
  const [unpublishing, setUnpublishing] = useState(false);
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

  async function unpublish() {
    setUnpublishing(true);
    setNotice('');
    try {
      const result = await execute?.({ type: 'unpublish', id: course.id });
      if (result?.id && !result.error) {
        window.location.replace(`#/human-courses/${encodeURIComponent(result.id)}/edit?from=my-posts`);
        return;
      }
      setNotice(result?.error || '게시글을 내리지 못했습니다.');
    } catch {
      setNotice('게시글을 내리지 못했습니다.');
    }
    setUnpublishing(false);
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

  return <div>
    <div className="actions">
      <div className="actions">
        <button className="primary" type="button" onClick={changeLike} disabled={course.mine || liking} aria-pressed={course.likedByMe} aria-busy={liking}>{liking ? '처리 중…' : course.likedByMe ? '좋아요 취소' : '좋아요 누르기'}</button>
        <span className="muted" aria-live="polite">좋아요 {course.likes}개</span>
      </div>
      <div className="actions course-actions-management">
        {course.mine && <>
          <a className="button" href={`#/human-courses/${course.id}/edit`}>수정하기</a>
          <button type="button" onClick={unpublish} disabled={unpublishing}>{unpublishing ? '내리는 중…' : '게시글 내리기'}</button>
        </>}
        {!course.reportedByMe && <button type="button" aria-haspopup="dialog" onClick={openReport} disabled={course.mine || liking}>게시글 신고</button>}
      </div>
    </div>
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
  </div>;
}
