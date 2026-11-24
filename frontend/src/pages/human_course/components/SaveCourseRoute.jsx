import { useEffect, useRef, useState } from 'react';
import { myPageApi } from '../../mypage/api/myPageApi';

export default function SaveCourseRoute({ postId }) {
  const dialog = useRef(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!saved) return;
    const timer = window.setTimeout(() => setSaved(false), 3500);
    return () => window.clearTimeout(timer);
  }, [saved]);

  function open() {
    setSaved(false);
    setError('');
    dialog.current.showModal();
  }

  async function save() {
    if (saving) return;
    setSaving(true);
    setError('');
    try {
      await myPageApi.saveHumanItinerary(postId);
      window.dispatchEvent(new Event('smart-travel-courses-updated'));
      dialog.current?.close();
      setSaved(true);
    } catch (error) {
      setError(error.message || '여행 경로를 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  }

  return <div className="course-save-action">
    <button type="button" className="primary" aria-haspopup="dialog" onClick={open}>여행 저장</button>
    <p className="status" role="status">{saved ? '여행 경로를 저장했어요.' : ''}</p>
    <dialog ref={dialog} className="course-report-dialog" aria-labelledby="course-save-title"
      onCancel={event => { if (saving) event.preventDefault(); }}>
      <h2 id="course-save-title">이 여행 경로로 숙소도 찾을까요?</h2>
      <p>경로만 저장해 두고 나중에 숙소를 찾을 수도 있어요.</p>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className="actions course-save-dialog__actions">
        {saving ? <button type="button" className="primary" disabled>바로 숙소 찾기</button>
          : <a className="button button-primary" href={`#/stays?courseId=${encodeURIComponent(`human-${postId}`)}`}>바로 숙소 찾기</a>}
        <button type="button" onClick={save} disabled={saving}>{saving ? '저장 중…' : '저장하기'}</button>
        <button type="button" onClick={() => dialog.current.close()} disabled={saving}>취소</button>
      </div>
    </dialog>
  </div>;
}
