import { useState } from 'react';
import { USER } from '../data/courseState';

export default function CourseComments({ course, execute, isAuthenticated = false }) {
  const [text, setText] = useState('');
  const [notice, setNotice] = useState('');
  const [deleting, setDeleting] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingBusy, setDeletingBusy] = useState(false);
  async function submitComment(event) {
    event.preventDefault();
    const value = text.trim();
    if (!value || submitting) return;
    setSubmitting(true);
    setNotice('');
    const result = await execute?.({ type: 'comment', id: course.id, commentId: crypto.randomUUID(), text: value });
    setNotice(result?.error || '댓글을 등록했습니다.');
    if (!result?.error) setText('');
    setSubmitting(false);
  }
  async function removeComment(commentId) {
    if (deletingBusy) return;
    setDeletingBusy(true);
    setNotice('');
    const result = await execute?.({ type: 'delete-comment', id: course.id, commentId });
    setNotice(result?.error || '댓글을 삭제했습니다.');
    setDeleting(null);
    setDeletingBusy(false);
  }
  return <section className="panel comment-panel" aria-labelledby="human-course-comments-title"><div className="panel-heading"><h2 id="human-course-comments-title">댓글 <span className="muted">({course.comments.length}개)</span></h2></div>
    <ul className="comment-list">{course.comments.length ? course.comments.map((comment) => <li key={comment.id}><div className="comment-copy"><strong className="comment-author">{comment.author}</strong><p>{comment.text}</p></div>{isAuthenticated && (comment.mine || comment.author === USER) && (deleting === comment.id ? <div className="actions comment-delete-confirm" role="group" aria-label={`${comment.author} 댓글 삭제 확인`}><span>삭제할까요?</span><button type="button" disabled={deletingBusy} onClick={() => removeComment(comment.id)}>{deletingBusy ? '삭제 중…' : '삭제 확인'}</button><button type="button" disabled={deletingBusy} onClick={() => setDeleting(null)}>취소</button></div> : <button type="button" onClick={() => { setDeleting(comment.id); setNotice(''); }}>내 댓글 삭제</button>)}</li>) : <li className="empty-comment">{isAuthenticated ? '첫 댓글을 남겨보세요.' : '등록된 댓글이 없습니다.'}</li>}</ul>
    {isAuthenticated && <form className="comment-form" onSubmit={submitComment}><div className="field"><label htmlFor={`human-course-comment-${course.id}`}>댓글 작성</label><textarea id={`human-course-comment-${course.id}`} name="comment" rows={3} maxLength={300} required value={text} onChange={(event) => setText(event.target.value)} placeholder="이 여행에 대한 댓글을 입력해 보세요." /><span className="comment-counter">{text.length}/300자</span></div><button className="primary" type="submit" disabled={submitting || !text.trim()}>{submitting ? '등록 중…' : '댓글 등록'}</button></form>}
    {notice && <p className="status" role={notice.includes('필요') || notice.includes('못') ? 'alert' : 'status'}>{notice}</p>}
  </section>;
}
